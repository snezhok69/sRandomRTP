package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class CommandHelp {

    public static void commandHelp(CommandSender sender) {
        if (!CommandUtils.requirePlayer(sender).isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.HELP)) return;
        Variables.getMessageService().send(sender, LoadMessages.commandhelp);
    }
}
