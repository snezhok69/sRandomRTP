package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class HandleFailedAttempt {
    public static void handleFailedAttempt(Player player, boolean loggingEnabled, int attemptNumber, int maxAttempts, Runnable retryCallback, TeleportRequestContext context) {
        if (context.isCancelled() || context.isCompleted()) {
            return;
        }

        if (attemptNumber >= maxAttempts) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("Maximum teleport attempts reached for player " + player.getName());
            }
            Variables.getMessageService().send(player, LoadMessages.locationNotFound);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "max attempts reached");
            Variables.getRuntimeState().setPlayerSearching(player, false);
            return;
        }

        if (loggingEnabled) {
            Bukkit.getLogger().info("Retrying teleport search for player " + player.getName() + " (attempt " + attemptNumber + ")");
        }
        retryCallback.run();
    }
}
