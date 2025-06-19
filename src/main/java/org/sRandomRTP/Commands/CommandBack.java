package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

public class CommandBack {
    public static void handleBackCommand(CommandSender sender) {
        Player player = (Player) sender;
        if (!(sender instanceof Player)) {
            sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
            return;
        }
        if (!player.hasPermission("sRandomRTP.Command.Back")) {
            List<String> formattedMessage = LoadMessages.nopermissioncommand;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }
        if (Variables.initialPositions.containsKey(player)) {
            Location initialLocation = Variables.initialPositions.get(player);
            player.teleportAsync(initialLocation);
            List<String> formattedMessage = LoadMessages.teleportBackSuccess;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
                Variables.initialPositions.remove(player);
            }
        } else {
            List<String> formattedMessage = LoadMessages.teleportBackFailure;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
        }
    }
}
