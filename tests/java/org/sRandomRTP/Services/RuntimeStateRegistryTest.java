package org.sRandomRTP.Services;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataTasks;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class RuntimeStateRegistryTest {

    @Test
    void clearPlayerRuntimeStateRemovesTransientSessionData() {
        RuntimeStateRegistry registry = new RuntimeStateRegistry();
        Player player = Mockito.mock(Player.class);
        WrappedTask task = Mockito.mock(WrappedTask.class);
        WrappedTask particleTask = Mockito.mock(WrappedTask.class);
        CommandSender sender = Mockito.mock(CommandSender.class);
        UUID playerId = java.util.UUID.randomUUID();

        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("runtime-state-player");
        when(player.getLocation()).thenReturn(new Location(null, 10.0D, 64.0D, 20.0D));

        registry.putTeleportTask(player, task);
        registry.putParticleTask(player, particleTask);
        registry.getCooldowns().put(playerId, System.currentTimeMillis());
        registry.getBiomeCooldowns().put(playerId, System.currentTimeMillis());
        registry.getPlayerSearchStatus().put(playerId, RuntimeStateRegistry.SearchPhase.SEARCHING);
        registry.getPlayerConfirmStatus().put(playerId, true);
        registry.getSenderSendMessage().put(playerId, sender);
        registry.getCommandSenderMap().put(playerId, sender);
        registry.getTargetWorlds().put(playerId, Mockito.mock(org.bukkit.World.class));
        registry.rememberInitialPosition(player);
        registry.getSuitableLocationFound().put(playerId, new java.util.concurrent.atomic.AtomicBoolean(true));

        registry.clearPlayerRuntimeState(player);

        assertFalse(registry.hasTeleportTask(player));
        assertNull(registry.getParticleTask(player));
        assertFalse(registry.isPlayerSearching(player));
        assertFalse(registry.getPlayerConfirmStatus().containsKey(playerId));
        assertFalse(registry.getSenderSendMessage().containsKey(playerId));
        assertFalse(registry.getCommandSenderMap().containsKey(playerId));
        assertFalse(registry.getTargetWorlds().containsKey(playerId));
        assertFalse(registry.getCooldowns().containsKey(playerId));
        assertFalse(registry.getBiomeCooldowns().containsKey(playerId));
        assertNull(registry.getInitialPosition(player));
        assertFalse(registry.getSuitableLocationFound().containsKey(playerId));
    }

    @Test
    void cleanExpiredCooldownsKeepsRecentEntries() {
        RuntimeStateRegistry registry = new RuntimeStateRegistry();
        long now = System.currentTimeMillis();
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        UUID oldBiomeId = UUID.randomUUID();
        UUID newBiomeId = UUID.randomUUID();

        registry.getCooldowns().put(oldId, now - 10_000L);
        registry.getCooldowns().put(newId, now - 100L);
        registry.getBiomeCooldowns().put(oldBiomeId, now - 10_000L);
        registry.getBiomeCooldowns().put(newBiomeId, now - 100L);

        registry.cleanExpiredCooldowns(1_000L);

        assertFalse(registry.getCooldowns().containsKey(oldId));
        assertTrue(registry.getCooldowns().containsKey(newId));
        assertFalse(registry.getBiomeCooldowns().containsKey(oldBiomeId));
        assertTrue(registry.getBiomeCooldowns().containsKey(newBiomeId));
    }

    @Test
    void portalHelpersTrackPortalsAndTasksWithoutTouchingGlobalMaps() {
        RuntimeStateRegistry registry = new RuntimeStateRegistry();
        PortalData portalData = new PortalData("player-one", "world", "home", 10.5, 64.0, 20.5, "circle");
        WrappedTask particlesTask = Mockito.mock(WrappedTask.class);
        WrappedTask triggerTask = Mockito.mock(WrappedTask.class);
        PortalDataTasks taskData = new PortalDataTasks(
                "player-one",
                "home",
                "trigger | particles",
                0L,
                20L,
                new Location(null, 10.5D, 64.0D, 20.5D),
                3,
                "task-ids",
                particlesTask,
                triggerTask,
                "circle"
        );

        registry.putPlayerPortal("player-one", "home", portalData);
        registry.putPortalTask("home", taskData);

        assertTrue(registry.hasPlayerPortal("player-one", "home"));
        assertEquals(portalData, registry.getPlayerPortal("player-one", "home"));
        assertEquals(taskData, registry.getPortalTask("home"));

        List<String> matches = registry.getMatchingPortalNames("player-one", "ho", 8);
        assertEquals(1, matches.size());
        assertEquals("home", matches.get(0));

        assertNotNull(registry.removePlayerPortal("player-one", "home"));
        assertFalse(registry.hasPlayerPortal("player-one", "home"));
        assertEquals(taskData, registry.removePortalTask("home"));
        assertNull(registry.getPortalTask("home"));
    }
}
