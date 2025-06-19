package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

import static org.sRandomRTP.DifferentMethods.Variables.chunkyAPI;

public class ChunkyCancelCommand {

    public static void chunkyCancelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sRandomRTP.Command.Chunky")) {
            List<String> formattedMessage = LoadMessages.nopermissioncommand;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }

        String worldName = player.getWorld().getName();

        if (!chunkyAPI.isRunning(worldName)) {
            List<String> formattedMessage = LoadMessages.chunkyradius_chunky;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }

        chunkyAPI.cancelTask(worldName);

        List<String> cancelMessage = LoadMessages.cancelMessage_chunky;
        for (String line : cancelMessage) {
            line = line.replace("%world%", worldName);
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            sender.sendMessage(formattedLine);
        }
    }
}