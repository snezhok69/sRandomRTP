package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages teleport-task cancellation and search-phase transitions.
 *
 * <p>All search-phase state is now stored in {@link RuntimeStateRegistry} via
 * {@link RuntimeStateRegistry.SearchPhase}. This class no longer holds its own
 * static {@code tasksCleanedButStatusActive} set — the {@code TASKS_CLEANED} phase
 * in the registry serves that role.</p>
 */
public class CleanupTasks {

    /** Shutdown-safe logger — never null even when plugin instance is being torn down. */
    private static final Logger LOG = Logger.getLogger(CleanupTasks.class.getName());

    public static void cleanupTasks(Player player, boolean loggingEnabled) {
        try {
            if (player == null) {
                return;
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (loggingEnabled) {
                LOG.info("[CleanupTasks] Starting cleanup for player " + player.getName());
                LOG.info("[CleanupTasks] Current teleportTasks size: " + state.getTeleportTasks().size());
                LOG.info("[CleanupTasks] Current playerSearchStatus: " + state.getPlayerSearchStatus());
                LOG.info("[CleanupTasks] Player in teleportTasks map: " + (state.hasTeleportTask(player) ? "yes" : "no"));
            }

            // Guard: if we already cleaned tasks for this player, wait for finalization.
            if (state.getSearchPhase(player) == RuntimeStateRegistry.SearchPhase.TASKS_CLEANED) {
                if (loggingEnabled) {
                    LOG.info("[CleanupTasks] Tasks already cleaned for player " + player.getName() + ", waiting for teleport completion");
                }
                return;
            }

            WrappedTask teleportTask = state.removeTeleportTask(player);
            if (teleportTask != null) {
                if (loggingEnabled) {
                    LOG.info("[CleanupTasks] Cancelling teleport task for player " + player.getName());
                }
                teleportTask.cancel();
                if (loggingEnabled) {
                    LOG.info("[CleanupTasks] Removed player from teleportTasks map after cancellation");
                }
            } else if (loggingEnabled) {
                LOG.info("[CleanupTasks] No teleport task found for player " + player.getName());
            }

            // Advance phase: tasks cleaned but search result handler hasn't fired yet.
            state.setSearchPhase(player, RuntimeStateRegistry.SearchPhase.TASKS_CLEANED);

            if (loggingEnabled) {
                LOG.info("[CleanupTasks] Marked tasks as cleaned for player " + player.getName() + ", will reset status after teleport");
            }

            WrappedTask particleTask = state.removeParticleTask(player);
            if (particleTask != null && !particleTask.isCancelled()) {
                if (loggingEnabled) {
                    LOG.info("[CleanupTasks] Cancelling particle task for player " + player.getName());
                }
                particleTask.cancel();
            }

            if (loggingEnabled) {
                LOG.info("[CleanupTasks] Teleportation tasks for the player " + player.getName() + " successfully cleared");
                LOG.info("[CleanupTasks] Final teleportTasks size: " + state.getTeleportTasks().size());
                LOG.info("[CleanupTasks] Final playerSearchStatus: " + state.getPlayerSearchStatus());
                LOG.info("[CleanupTasks] Player still in teleportTasks map: " + (state.hasTeleportTask(player) ? "yes" : "no"));
            }
        } catch (RuntimeException e) {
            // On error, reset phase to IDLE so the player is not permanently stuck.
            RuntimeStateRegistry rs = Variables.getRuntimeState();
            if (rs != null) rs.setPlayerSearching(player, false);
            LOG.log(Level.SEVERE, "[CleanupTasks] Error when clearing teleportation tasks", e);
        }
    }

    /**
     * Resets phase to IDLE for the given player.
     * Called from {@code PortalAndEffectsListener.onPlayerQuit()} for players who disconnect mid-teleport.
     */
    public static void clearForPlayer(UUID playerId) {
        // The SearchPhase is stored in RuntimeStateRegistry and will be cleared by
        // clearPlayerRuntimeState() — this method is kept for call-site compatibility.
        // No-op here because clearPlayerRuntimeState clears playerSearchPhase.
    }

    /** Clears all tracked state on plugin shutdown. */
    public static void clearAll() {
        // No-op — all state lives in RuntimeStateRegistry, which is torn down via onDisable().
    }

    public static void finalizeTeleportStatus(Player player, boolean loggingEnabled) {
        try {
            if (player == null) {
                return;
            }
            if (loggingEnabled) {
                LOG.info("[CleanupTasks] Finalizing teleport status for player " + player.getName());
            }

            // Transition back to IDLE: sets playerSearching=false and clears TASKS_CLEANED.
            RuntimeStateRegistry rs = Variables.getRuntimeState();
            if (rs != null) rs.setPlayerSearching(player, false);
            Variables.clearTeleportFlags(player);

            if (loggingEnabled) {
                LOG.info("[CleanupTasks] Successfully reset playerSearchStatus to false for player " + player.getName() + " after teleport completion");
            }
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "[CleanupTasks] Error when finalizing teleport status", e);
        }
    }
}
