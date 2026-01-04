package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Rtp.RtpRtp;

import java.util.HashMap;
import java.util.Map;

public class CleanupTasks {
    private static Map<String, Boolean> tasksCleanedButStatusActive = new HashMap<>();

    public static void cleanupTasks(Player player, boolean loggingEnabled) {
        try {
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Starting cleanup for player " + player.getName());
                Variables.getInstance().getLogger().info("[CleanupTasks] Current teleportTasks size: " + Variables.teleportTasks.size());
                Variables.getInstance().getLogger().info("[CleanupTasks] Current playerSearchStatus: " + Variables.playerSearchStatus);
                Variables.getInstance().getLogger().info("[CleanupTasks] Player in teleportTasks map: " + (Variables.teleportTasks.containsKey(player) ? "yes" : "no"));
            }

            if (tasksCleanedButStatusActive.getOrDefault(player.getName(), false)) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Tasks already cleaned for player " + player.getName() + ", waiting for teleport completion");
                }
                return;
            }

            WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
            if (checkProximityTaskTask != null) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling checkProximityTaskTask for player " + player.getName());
                }
                checkProximityTaskTask.cancel();
                Variables.teleportTasks.remove(player);
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Removed player from teleportTasks map after cancellation");
                }
            } else if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] No checkProximityTaskTask found for player " + player.getName());
            }

            tasksCleanedButStatusActive.put(player.getName(), true);
            
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Marked tasks as cleaned for player " + player.getName() + ", will reset status after teleport");
            }

            if (player.getName().equals(Variables.currentSearchingPlayer)) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Clearing currentSearchingPlayer: " + Variables.currentSearchingPlayer);
                }
                Variables.currentSearchingPlayer = null;
            }

            WrappedTask task = Variables.teleportTasks.get(player);
            if (task != null && !task.isCancelled()) {
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling additional task for player " + player.getName());
                }
                task.cancel();
                Variables.teleportTasks.remove(player);
                if (loggingEnabled) {
                    Variables.getInstance().getLogger().info("[CleanupTasks] Removed player from teleportTasks map after additional cancellation");
                }
            }

            for (Map.Entry<Player, WrappedTask> entry : new HashMap<>(Variables.teleportTasks).entrySet()) {
                if (entry.getKey().equals(player) && entry.getValue() != null && !entry.getValue().isCancelled()) {
                    if (loggingEnabled) {
                        Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling task from map iteration for player " + player.getName());
                    }
                    entry.getValue().cancel();
                    Variables.teleportTasks.remove(entry.getKey());
                    if (loggingEnabled) {
                        Variables.getInstance().getLogger().info("[CleanupTasks] Removed player from teleportTasks map after map iteration");
                    }
                }
            }

            if (Variables.particleTasks != null && Variables.particleTasks.containsKey(player)) {
                WrappedTask particleTask = Variables.particleTasks.get(player);
                if (particleTask != null && !particleTask.isCancelled()) {
                    if (loggingEnabled) {
                        Variables.getInstance().getLogger().info("[CleanupTasks] Cancelling particle task for player " + player.getName());
                    }
                    particleTask.cancel();
                    Variables.particleTasks.remove(player);
                }
            }

            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Teleportation tasks for the player " + player.getName() + " successfully cleared");
                Variables.getInstance().getLogger().info("[CleanupTasks] Final teleportTasks size: " + Variables.teleportTasks.size());
                Variables.getInstance().getLogger().info("[CleanupTasks] Final playerSearchStatus: " + Variables.playerSearchStatus);
                Variables.getInstance().getLogger().info("[CleanupTasks] Player still in teleportTasks map: " + (Variables.teleportTasks.containsKey(player) ? "yes" : "no"));
            }
        } catch (Exception e) {
            if (loggingEnabled) {
                Variables.getInstance().getLogger().severe("[CleanupTasks] Error when clearing teleportation tasks: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static void finalizeTeleportStatus(Player player, boolean loggingEnabled) {
        try {
            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Finalizing teleport status for player " + player.getName());
            }

            Variables.playerSearchStatus.put(player.getName(), false);
            tasksCleanedButStatusActive.remove(player.getName());

            Variables.clearTeleportFlags(player);

            if (loggingEnabled) {
                Variables.getInstance().getLogger().info("[CleanupTasks] Successfully reset playerSearchStatus to false for player " + player.getName() + " after teleport completion");
            }
        } catch (Exception e) {
            if (loggingEnabled) {
                Variables.getInstance().getLogger().severe("[CleanupTasks] Error when finalizing teleport status: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
