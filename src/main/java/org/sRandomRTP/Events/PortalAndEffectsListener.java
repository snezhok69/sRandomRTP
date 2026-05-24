package org.sRandomRTP.Events;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.sRandomRTP.Chunk.ChunkWarmManager;
import org.sRandomRTP.Commands.portal.PortalTriggerHandler;
import org.sRandomRTP.Cooldowns.CooldownManager;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveAllBossBars;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Teleport.CleanupTasks;
import org.sRandomRTP.DifferentMethods.Teleport.PerformTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consolidated listener for portal-block protection, particle effects, and player quit cleanup.
 * Replaces the former PlayerBreakBlockPortal and PlayerParticles classes.
 */
public class PortalAndEffectsListener implements Listener {

    // ── Portal block protection ─────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGH)
    public void onPortalBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block == null || block.getWorld() == null) return;
        if (isPortalBlock(block) && org.sRandomRTP.Services.PortalSettings.current().isPortalBlocksProtected()) {
            event.setCancelled(true);
            Variables.getMessageService().send(event.getPlayer(), LoadMessages.error_break_portal_block);
        }
    }

    private boolean isPortalBlock(Block block) {
        return Variables.getRuntimeState().isPortalBlockAtLocation(
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    // ── Particle effects ────────────────────────────────────────────────────

    /**
     * Starts a portal-enter particle effect loop for the given player.
     * Called by {@link org.sRandomRTP.DifferentMethods.Teleport.PerformTeleport}.
     */
    public static void startParticleEffect(Player player) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        // Cancel any existing particle task for this player before starting a new one —
        // prevents task leak when this method is called twice for the same player.
        WrappedTask existing = state.removeParticleTask(player);
        if (existing != null && !existing.isCancelled()) {
            existing.cancel();
        }
        final AtomicInteger ticks = new AtomicInteger(0);
        // Use cached config values instead of reading from disk on every call
        final int duration = Variables.configCache.particleDuration;
        final boolean visibleToPlayerOnly = Variables.configCache.particleVisibleToPlayerOnly;
        final int count = Variables.configCache.particleCount;
        final double offsetX = Variables.configCache.particleOffsetX;
        final double offsetY = Variables.configCache.particleOffsetY;
        final double offsetZ = Variables.configCache.particleOffsetZ;
        final double extra = Variables.configCache.particleExtra;
        final List<Particle> particleTypes = Variables.configCache.particleTypes;

        // Use AtomicReference so the task lambda can cancel itself safely even if the
        // scheduler fires before putParticleTask() stores the reference in the registry.
        final java.util.concurrent.atomic.AtomicReference<WrappedTask> taskRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        WrappedTask task = Variables.getFoliaLib().getImpl().runAtEntityTimer(player, () -> {
            if (ticks.get() >= duration) {
                WrappedTask self = taskRef.get();
                if (self != null) {
                    self.cancel();
                }
                state.removeParticleTask(player);
                return;
            }
            Location location = player.getLocation();
            for (Particle particleType : particleTypes) {
                if (visibleToPlayerOnly) {
                    player.spawnParticle(particleType, location, count, offsetX, offsetY, offsetZ, extra);
                } else {
                    player.getWorld().spawnParticle(particleType, location, count, offsetX, offsetY, offsetZ, extra);
                }
            }
            ticks.incrementAndGet();
        }, 1L, 1L);

        taskRef.set(task);
        state.putParticleTask(player, task);
    }

    // ── Player quit cleanup ─────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        WrappedTask searchTask = state.getParticleTask(player);
        if (searchTask != null) {
            searchTask.cancel();
            state.removeParticleTask(player);
        }
        Variables.getAdminBarService().cleanupPlayer(player);
        // Dismiss any active countdown boss bar before clearing registry state —
        // removeBossBar is idempotent (safe when no bar is active).
        RemoveAllBossBars.removeBossBar(player);
        state.clearPlayerRuntimeState(player);
        // Clear insidePlayers and teleportCooldown for the disconnecting player to prevent memory leak
        PortalTriggerHandler triggerHandler = Variables.getPortalTriggerHandler();
        if (triggerHandler != null) triggerHandler.handlePlayerQuit(player.getUniqueId());
        // Invalidate cooldown permission cache so offline-player entries don't accumulate
        CooldownManager.instance().invalidatePermissionCache(player.getUniqueId());
        // Clear teleport in-progress guard and cleanup flags so a player who disconnects
        // mid async-teleport does not permanently block future teleports.
        PerformTeleport.clearInProgressForPlayer(player.getUniqueId());
        CleanupTasks.clearForPlayer(player.getUniqueId());
        // Refund any pending economy charge if the player disconnects mid-search.
        // refund(UUID) is idempotent — no-ops if no pending payment exists.
        EconomyPaymentManager.refund(player.getUniqueId());
        // Drop entries from the recent-chunk cache and active-request map so they
        // do not accumulate UUID keys for players who never come back during a
        // server uptime (a slow but real leak on high-churn servers).
        TeleportRequestManager.clearForPlayer(player.getUniqueId());
        // Same idea for the chunk-warm bookkeeping map.
        ChunkWarmManager.removePlayer(player.getUniqueId());
    }
}
