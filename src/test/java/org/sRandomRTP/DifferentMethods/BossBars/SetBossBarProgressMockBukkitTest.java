package org.sRandomRTP.DifferentMethods.BossBars;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Services.PluginContext;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Utils.PlayerResourceMap;

import org.bukkit.configuration.file.YamlConfiguration;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the TOCTOU fix in SetBossBarProgress: concurrent calls for the same player
 * must produce exactly one BossBar (not two), using the computeIfAbsent atomic pattern.
 */
@Tag("mockbukkit")
class SetBossBarProgressMockBukkitTest {

    private ServerMock server;
    private PlayerMock player;
    private RuntimeStateRegistry runtimeState;
    private ConfigCache previousCache;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        player = server.addPlayer("bar-tester");

        runtimeState = new RuntimeStateRegistry();

        // Build a minimal ConfigRegistry that returns a bossbar config with valid values
        YamlConfiguration bossBarCfg = new YamlConfiguration();
        bossBarCfg.set("teleport.bar-color", "BLUE");
        bossBarCfg.set("teleport.bar-style", "SOLID");

        ConfigRegistry configRegistry = Mockito.mock(ConfigRegistry.class);
        Mockito.when(configRegistry.getBossBarFile()).thenReturn(bossBarCfg);

        PluginContext pluginContext = Mockito.mock(PluginContext.class);
        Mockito.when(pluginContext.getConfigRegistry()).thenReturn(configRegistry);

        Field pcField = Variables.class.getDeclaredField("pluginContext");
        pcField.setAccessible(true);
        pcField.set(null, pluginContext);

        Field rsField = Variables.class.getDeclaredField("runtimeState");
        rsField.setAccessible(true);
        rsField.set(null, runtimeState);

        previousCache = Variables.configCache;
        Variables.configCache = ConfigCache.DEFAULT;
    }

    @AfterEach
    void tearDown() throws Exception {
        Variables.configCache = previousCache;
        Field pcField = Variables.class.getDeclaredField("pluginContext");
        pcField.setAccessible(true);
        pcField.set(null, null);
        Field rsField = Variables.class.getDeclaredField("runtimeState");
        rsField.setAccessible(true);
        rsField.set(null, null);
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void singleCallCreatesOneBossBar() {
        SetBossBarProgress.setBossBarProgress(player, player, 1.0, "Loading...");

        PlayerResourceMap<BossBar> bars = runtimeState.getBossBars();
        assertEquals(1, bars.size(), "exactly one BossBar should exist after single call");
    }

    @Test
    void secondCallForSamePlayerUpdatesExistingBar() {
        SetBossBarProgress.setBossBarProgress(player, player, 1.0, "Loading...");
        SetBossBarProgress.setBossBarProgress(player, player, 0.5, "Half done");

        PlayerResourceMap<BossBar> bars = runtimeState.getBossBars();
        assertEquals(1, bars.size(), "second call must not create a second BossBar");
        BossBar bar = bars.get(player);
        assertNotNull(bar);
        assertEquals(0.5, bar.getProgress(), 0.001, "progress should be updated to latest value");
    }

    @Test
    void concurrentCallsForSamePlayerCreateExactlyOneBar() throws InterruptedException {
        int threads = 8;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    SetBossBarProgress.setBossBarProgress(player, player, 1.0, "Searching...");
                } catch (InterruptedException ignored) {
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(1, runtimeState.getBossBars().size(),
                "concurrent calls for the same player must produce exactly one BossBar (no leaks)");
    }

    @Test
    void callsForDifferentPlayersCreateSeparateBars() {
        PlayerMock player2 = server.addPlayer("bar-tester-2");

        SetBossBarProgress.setBossBarProgress(player, player, 1.0, "Player 1");
        SetBossBarProgress.setBossBarProgress(player2, player2, 1.0, "Player 2");

        assertEquals(2, runtimeState.getBossBars().size(), "each player should have their own BossBar");
    }
}
