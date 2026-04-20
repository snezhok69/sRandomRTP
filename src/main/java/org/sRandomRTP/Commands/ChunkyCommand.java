package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import static org.sRandomRTP.DifferentMethods.Variables.chunkyAPI;

public class ChunkyCommand {

    public static void chunkyCommand(CommandSender sender, String[] args) {
        Player player = CommandUtils.requirePlayer(sender).orElse(null);
        if (player == null) return;
        if (!CommandUtils.checkPermission(sender, Permissions.CHUNKY)) return;

        if (args.length < 2) {
            player.sendMessage(ChatUtils.PLUGIN_NAME + " §cUsage: /rtp chunky <radius>");
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
