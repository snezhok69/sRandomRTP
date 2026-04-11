package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpNear;

public class CommandNear extends AbstractRtpCommand {

    public static void commandNear(CommandSender sender) {
        new CommandNear().execute(sender);
    }

    @Override
    protected String requiredPermission() {
        return Permissions.NEAR;
    }

    @Override
    protected boolean additionalChecks(Player player, CommandSender sender, World world, boolean loggingEnabled) {
        if (Bukkit.getOnlinePlayers().size() <= 1) {
            Variables.getMessageService().send(player,LoadMessages.noplayerservenear);
            return false;
        }
        if (world.getPlayers().size() <= 1) {
            Variables.getMessageService().send(player,LoadMessages.noplayerworldnear, "%world%", world.getName());
            return false;
        }
        return true;
    }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> RtpRtpNear.rtpRtpNear(sender);
    }
}
