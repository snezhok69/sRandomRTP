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
import org.sRandomRTP.Commands.CommandSetPortal;
import org.sRandomRTP.Cooldowns.CooldownManager;
import org.sRandomRTP.DifferentMethods.Teleport.CleanupTasks;
import org.sRandomRTP.DifferentMethods.Teleport.PerformTeleport;
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
        if (isPortalBlock(block) && CommandSetPortal.isPortalBlocksProtected()) {
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
        // Используем кешированные значения конфига вместо чтения с диска при каждом вызове
        final int duration = Variables.cachedParticleDuration;
        final boolean visibleToPlayerOnly = Variables.cachedParticleVisibleToPlayerOnly;
        final int count = Variables.cachedParticleCount;
        final double offsetX = Variables.cachedParticleOffsetX;
        final double offsetY = Variables.cachedParticleOffsetY;
        final double offsetZ = Variables.cachedParticleOffsetZ;
        final double extra = Variables.cachedParticleExtra;
        final List<Particle> particleTypes = Variables.cachedParticleTypes;

        WrappedTask task = Variables.getFoliaLib().getImpl().runAtEntityTimer(player, () -> {
            if (ticks.get() >= duration) {
                WrappedTask particleTask = state.getParticleTask(player);
                if (particleTask != null) {
                    particleTask.cancel();
                    state.removeParticleTask(player);
                }
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

        state.putParticleTask(player, task);
    }

    // ── Player quit cleanup ─────────────────────────────────────────────────

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        WrappedTask searchTask = state.getParticleTask(player);
        if (searchTask != null) {
            searchTask.cancel();
            state.removeParticleTask(player);
        }
        Variables.getAdminBarService().cleanupPlayer(player);
        state.clearPlayerRuntimeState(player);
        // Фикс утечки памяти: очищаем insidePlayers и teleportCooldown для вышедшего игрока
        CommandSetPortal.handlePlayerQuit(player.getUniqueId());
        // Инвалидируем кеш прав кулдауна, чтобы не накапливать записи оффлайн-игроков
        CooldownManager.invalidateCache(player.getUniqueId());
        // Очищаем in-progress guard телепортации и статус очистки задач,
        // чтобы игрок, отключившийся в середине async-телепорта, не блокировал будущие телепорты.
        PerformTeleport.clearInProgressForPlayer(player.getUniqueId());
        CleanupTasks.clearForPlayer(player.getUniqueId());
    }
}
