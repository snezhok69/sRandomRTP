package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CleanupTasks {
    private static final Set<UUID> tasksCleanedButStatusActive = ConcurrentHashMap.newKeySet();

    public static void cleanupTasks(Player player, boolean loggingEnabled) {
        try {
            if (player == null) {
                return;
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Starting cleanup for player " + player.getName());
                Variables.getInstance().getLogger().info("[CleanupTasks] Current teleportTasks size: " + state.getTeleportTasks().size());
                Variables.getInstance().getLogger().info("[CleanupTasks] Current playerSearchStatus: " + state.getPlayerSearchStatus());
                Variables.getInstance().getLogger().info("[CleanupTasks] Player in teleportTasks map: " + (state.hasTeleportTask(player) ? "yes" : "no"));
            }

            if (tasksCleanedButStatusActive.contains(player.getUniqueId())) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Tasks already cleaned for player " + player.getName() + ", waiting for teleport completion");
                }
                return;
            }

            WrappedTask teleportTask = state.removeTeleportTask(player);
            if (teleportTask != null) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling teleport task for player " + player.getName());
                }
                teleportTask.cancel();
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Removed player from teleportTasks map after cancellation");
                }
            } else if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] No teleport task found for player " + player.getName());
            }

            tasksCleanedButStatusActive.add(player.getUniqueId());
            
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Marked tasks as cleaned for player " + player.getName() + ", will reset status after teleport");
            }

            WrappedTask particleTask = state.removeParticleTask(player);
            if (particleTask != null && !particleTask.isCancelled()) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling particle task for player " + player.getName());
                }
                particleTask.cancel();
            }

            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Teleportation tasks for the player " + player.getName() + " successfully cleared");
                Variables.getInstance().getLogger().info("[CleanupTasks] Final teleportTasks size: " + state.getTeleportTasks().size());
                Variables.getInstance().getLogger().info("[CleanupTasks] Final playerSearchStatus: " + state.getPlayerSearchStatus());
                Variables.getInstance().getLogger().info("[CleanupTasks] Player still in teleportTasks map: " + (state.hasTeleportTask(player) ? "yes" : "no"));
            }
        } catch (RuntimeException e) {
            tasksCleanedButStatusActive.remove(player.getUniqueId());
            Variables.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                    "[CleanupTasks] Error when clearing teleportation tasks", e);
        }
    }

    /**
     * Clears all tracked state. Called from {@code Main.onDisable()} and from
     * {@code PortalAndEffectsListener.onPlayerQuit()} for players who disconnect mid-teleport.
     */
    public static void clearForPlayer(UUID playerId) {
        tasksCleanedButStatusActive.remove(playerId);
    }

    /** Clears all tracked state on plugin shutdown. */
    public static void clearAll() {
        tasksCleanedButStatusActive.clear();
    }

    public static void finalizeTeleportStatus(Player player, boolean loggingEnabled) {
        try {
            if (player == null) {
                return;
            }
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Finalizing teleport status for player " + player.getName());
            }

            Variables.getRuntimeState().setPlayerSearching(player, false);
            tasksCleanedButStatusActive.remove(player.getUniqueId());

            Variables.clearTeleportFlags(player);

            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Successfully reset playerSearchStatus to false for player " + player.getName() + " after teleport completion");
            }
        } catch (RuntimeException e) {
            Variables.getInstance().getLogger().log(java.util.logging.Level.SEVERE,
                    "[CleanupTasks] Error when finalizing teleport status", e);
        }
    }
}
