package org.sRandomRTP.DifferentMethods.Teleport;

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

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Verifies the null-safety fix in TeleportExecutionService:
 * when RuntimeStateRegistry is null (plugin disabled mid-flight),
 * calling execute() with an offline player must not throw NPE.
 */
@Tag("mockbukkit")
class TeleportExecutionServiceNullStateMockBukkitTest {

    private ServerMock server;
    private RuntimeStateRegistry previousState;
    private Object previousPluginContext;
    private ConfigCache previousCache;

    @BeforeEach
    void setUp() throws Exception {
        server = MockBukkit.mock();

        Field rsField = Variables.class.getDeclaredField("runtimeState");
        rsField.setAccessible(true);
        previousState = (RuntimeStateRegistry) rsField.get(null);
        rsField.set(null, null);

        Field pcField = Variables.class.getDeclaredField("pluginContext");
        pcField.setAccessible(true);
        previousPluginContext = pcField.get(null);

        // Minimal config registry so ConfigCache reads don't NPE
        ConfigRegistry configRegistry = Mockito.mock(ConfigRegistry.class);
        Mockito.when(configRegistry.getChunkFile()).thenReturn(null);
        PluginContext pluginContext = Mockito.mock(PluginContext.class);
        Mockito.when(pluginContext.getConfigRegistry()).thenReturn(configRegistry);
        pcField.set(null, pluginContext);

        previousCache = Variables.configCache;
        Variables.configCache = ConfigCache.DEFAULT;
    }

    @AfterEach
    void tearDown() throws Exception {
        Variables.configCache = previousCache;
        Field rsField = Variables.class.getDeclaredField("runtimeState");
        rsField.setAccessible(true);
        rsField.set(null, previousState);
        Field pcField = Variables.class.getDeclaredField("pluginContext");
        pcField.setAccessible(true);
        pcField.set(null, previousPluginContext);
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    /**
     * When player or resolution is null, execute() must return early without
     * touching RuntimeStateRegistry at all.
     */
    @Test
    void executeWithNullPlayerDoesNotThrow() {
        assertDoesNotThrow(() ->
                TeleportExecutionService.execute(null, null, false, null),
                "null player must trigger early return, not NPE");
    }

    @Test
    void executeWithNullResolutionDoesNotThrow() {
        PlayerMock player = server.addPlayer("tes-tester");
        assertDoesNotThrow(() ->
                TeleportExecutionService.execute(player, null, false, null),
                "null resolution must trigger early return, not NPE");
    }

    /**
     * When player is offline and state is null, the online-check path
     * calls cancelRequest and then reads getRuntimeState().
     * With our fix (if (state != null)) this must not throw.
     */
    @Test
    void executeOfflinePlayerWithNullStateMustNotThrow() {
        PlayerMock player = server.addPlayer("tes-offline");
        player.disconnect();

        RtpCandidateResolution resolution = new RtpCandidateResolution(
                server.getWorlds().get(0), null, 0, 64, 0, null, null, null, null,
                0, true, false, true, 0L, 0L);

        // Should not throw NPE even though RuntimeStateRegistry is null
        assertDoesNotThrow(() ->
                TeleportExecutionService.execute(player, resolution, false, null));
    }
}
