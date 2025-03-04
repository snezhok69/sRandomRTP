package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

import static org.sRandomRTP.DifferentMethods.Variables.chunkyAPI;

public class ChunkyCommand {

    public static void chunkyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("sRandomRTP.Cooldown.Chunky")) {
            List<String> formattedMessage = LoadMessages.nopermissioncommand;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Variables.pluginName + " §cUsage: /rtp chunky <radius>");
            return;
        }

        double radius;
        try {
            radius = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(Variables.pluginName + " §cThe radius should be a number!");
            return;
        }

        chunkyAPI.startTask(
                player.getWorld().getName(),
                "square",
                player.getLocation().getX(),
                player.getLocation().getZ(),
                radius,
                0,
                "default"
        );

        List<String> successMessage = LoadMessages.successMessage_chunky;
        for (String line : successMessage) {
            line = line.replace("%radius%", String.valueOf(radius));
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            sender.sendMessage(formattedLine);
        }
    }
}
