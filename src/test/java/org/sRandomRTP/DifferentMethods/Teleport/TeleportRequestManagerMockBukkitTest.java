package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.Mockito;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Services.PluginContext;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class TeleportRequestManagerMockBukkitTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();
        player = server.addPlayer("teleport-tester");

        YamlConfiguration teleportConfig = new YamlConfiguration();
        teleportConfig.set("teleport.teleport-timeout.enabled", true);
        teleportConfig.set("teleport.teleport-timeout.attempt-timeout-ms", 1_500L);
        teleportConfig.set("teleport.teleport-timeout.total-timeout-ms", 4_500L);

        ConfigRegistry configRegistry = Mockito.mock(ConfigRegistry.class);
        when(configRegistry.getTeleportFile()).thenReturn(teleportConfig);

        PluginContext pluginContext = Mockito.mock(PluginContext.class);
        when(pluginContext.getConfigRegistry()).thenReturn(configRegistry);

        Field field = Variables.class.getDeclaredField("pluginContext");
        field.setAccessible(true);
        field.set(null, pluginContext);

        // Phase 2.2: TeleportRequestManager now reads timeout values from ConfigCache,
        // not from live YAML — populate the cache with the same values the test expects.
        Variables.configCache = org.sRandomRTP.Services.ConfigCache.buildFrom(configRegistry);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (player != null) {
            TeleportRequestManager.cancelRequest(player.getUniqueId(), false, "test cleanup");
        }
        Field field = Variables.class.getDeclaredField("pluginContext");
        field.setAccessible(true);
        field.set(null, null);
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    @Test
    void beginRequestCancelsPreviousActiveRequest() {
        TeleportRequestContext first = TeleportRequestManager.beginRequest(player, false);

        assertSame(first, TeleportRequestManager.getContext(player.getUniqueId()));
        assertFalse(first.isCancelled());
        assertEquals(1_500L, first.getPerAttemptTimeoutMillis());

        TeleportRequestContext second = TeleportRequestManager.beginRequest(player, false);

        assertTrue(first.isCancelled(), "first request should be cancelled before replacement");
        assertSame(second, TeleportRequestManager.getContext(player.getUniqueId()));
        assertNotSame(first, second);
        assertFalse(second.isCancelled());

        TeleportRequestManager.completeRequest(second, false);

        assertNull(TeleportRequestManager.getContext(player.getUniqueId()));
        assertTrue(second.isCompleted());
    }
}
