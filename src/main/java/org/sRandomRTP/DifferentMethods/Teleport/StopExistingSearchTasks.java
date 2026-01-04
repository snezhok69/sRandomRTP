package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;

public class StopExistingSearchTasks {
    public static void stopExistingSearchTasks(Player player, boolean loggingEnabled) {
        if (player == null) {
            return;
        }
        if (loggingEnabled) {
            Bukkit.getLogger().info("Stopping all search tasks for player " + player.getName());
        }
        TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "external cancellation");
        Variables.teleportTasks.remove(player);
    }
}
