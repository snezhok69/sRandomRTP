package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class CommandVersion {
    public static void commandVersion(CommandSender sender) {
        if (!sender.hasPermission(Permissions.VERSION)) {
            Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
            return;
        }

        Variables.getReleaseCheckService().sendVersionStatus(sender);
    }
}
