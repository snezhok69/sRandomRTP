package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportCancellationSupport;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class CommandCancel {
    public static void commandRtpCancel(CommandSender sender) {
        try {
            java.util.Optional<Player> playerOpt = CommandUtils.requirePlayer(sender);
            if (!playerOpt.isPresent()) return;
            if (!CommandUtils.checkPermission(sender, Permissions.CANCEL)) return;

            Player player = playerOpt.get();
            boolean loggingEnabled = Variables.isLoggingEnabled();
            if (!TeleportCancellationSupport.hasActiveTeleport(player)) {
                Variables.getMessageService().send(sender, LoadMessages.nosearching);
                return;
            }

            TeleportCancellationSupport.cancelAndNotify(
                    player,
                    loggingEnabled,
                    "cancel command",
                    LoadMessages.teleportcancel,
                    true
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(CommandCancel.class, e);
        }
    }
}
