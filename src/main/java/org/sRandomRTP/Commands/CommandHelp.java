package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

public class CommandHelp {

    public static void commandhelp(CommandSender sender) {
        try {
        Player player = (Player) sender;
        if (!player.hasPermission("sRandomRTP.Command.Help")) {
            List<String> formattedMessage = LoadMessages.nopermissioncommand;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }
        List<String> formattedMessage = LoadMessages.commandhelp;
        for (String line : formattedMessage) {
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            sender.sendMessage(formattedLine);
        }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}


