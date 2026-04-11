package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import static org.sRandomRTP.DifferentMethods.Variables.chunkyAPI;

public class ChunkyCommand {

    public static void chunkyCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Variables.sendPlayersOnly(sender);
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(Permissions.CHUNKY)) {
            Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
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
            Variables.getMessageService().send(sender, LoadMessages.chunkyradius_chunky);
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

        Variables.getMessageService().send(sender, LoadMessages.successMessage_chunky,
                "%radius%", String.valueOf(radius));
    }
}
