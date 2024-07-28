package org.sRandomRTP.DifferentMethods;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Files.LoadMessages;
import java.util.List;

public class PlayerSearchStatus {
    public static boolean playerSearchStatus(Player player, CommandSender sender) {
        try {
        if (Variables.playerSearchStatus.getOrDefault(player.getName(), false)) {
            List<String> formattedMessage = LoadMessages.teleportationinprogress;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return true;
        }
        return false;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}