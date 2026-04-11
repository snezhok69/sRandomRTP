package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import static org.sRandomRTP.DifferentMethods.Variables.chunkyAPI;

public class ChunkyCancelCommand {

    public static void chunkyCancelCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            Variables.sendPlayersOnly(sender);
            return;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(Permissions.CHUNKY)) {
            Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
            return;
        }

        String worldName = player.getWorld().getName();

        if (!chunkyAPI.isRunning(worldName)) {
            Variables.getMessageService().send(sender, LoadMessages.chunkyradius_chunky);
            return;
        }

        chunkyAPI.cancelTask(worldName);

        Variables.getMessageService().send(sender, LoadMessages.cancelMessage_chunky, "%world%", worldName);
    }
}
