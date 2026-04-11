package org.sRandomRTP.Commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Rtp.RadiusConfigurableRtpHandler;

public class CommandFar extends AbstractRtpCommand {

    public static void commandFar(CommandSender sender) {
        new CommandFar().execute(sender);
    }

    @Override
    protected String requiredPermission() { return Permissions.FAR; }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> RadiusConfigurableRtpHandler.rtpFar(sender, world);
    }
}
