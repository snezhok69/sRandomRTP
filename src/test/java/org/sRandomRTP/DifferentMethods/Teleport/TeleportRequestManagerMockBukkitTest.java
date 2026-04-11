package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.sRandomRTP.DifferentMethods.Variables;

import static org.junit.jupiter.api.Assertions.*;

class TeleportRequestManagerMockBukkitTest {

    private ServerMock server;
    private PlayerMock player;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        player = server.addPlayer("teleport-tester");

        YamlConfiguration teleportConfig = new YamlConfiguration();
        teleportConfig.set("teleport.teleport-timeout.enabled", true);
        teleportConfig.set("teleport.teleport-timeout.attempt-timeout-ms", 1_500L);
        teleportConfig.set("teleport.teleport-timeout.total-timeout-ms", 4_500L);
        Variables.teleportfile = teleportConfig;
        Variables.econ = null;
    }

    @AfterEach
    void tearDown() {
        if (player != null) {
            TeleportRequestManager.cancelRequest(player.getUniqueId(), false, "test cleanup");
        }
        Variables.teleportfile = null;
        Variables.econ = null;
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
