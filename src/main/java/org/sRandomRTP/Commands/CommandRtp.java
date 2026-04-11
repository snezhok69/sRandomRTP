package org.sRandomRTP.Commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Rtp.AbstractRtpHandler;

public class CommandRtp extends AbstractRtpCommand {

    public static void commandRtp(CommandSender sender) {
        new CommandRtp().execute(sender);
    }

    @Override
    protected String requiredPermission() { return Permissions.RTP; }

    @Override
    protected boolean clearsTeleportFlags() { return true; }

    @Override
    protected Runnable buildAction(CommandSender sender, Player player, World world) {
        return () -> AbstractRtpHandler.launch(sender, world);
    }
}
