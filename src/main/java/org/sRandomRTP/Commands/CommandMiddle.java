package org.sRandomRTP.Commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Rtp.RadiusConfigurableRtpHandler;

public class CommandMiddle extends AbstractRtpCommand {

    public static void commandMiddle(CommandSender sender) {
        new CommandMiddle().execute(sender);
    }

    @Override
    protected String requiredPermission() { return Permissions.MIDDLE; }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> RadiusConfigurableRtpHandler.rtpMiddle(sender, world);
    }
}
