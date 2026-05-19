package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.LocalFeatureGate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandHelp {

    public static void commandHelp(CommandSender sender) {
        if (!CommandUtils.requirePlayer(sender).isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.HELP)) return;
        Variables.getMessageService().send(sender, visibleHelpLines());
    }

    private static List<String> visibleHelpLines() {
        if (LoadMessages.commandhelp == null) {
            return Collections.emptyList();
        }
        if (LocalFeatureGate.isLocalAdminBarsEnabled()) {
            return LoadMessages.commandhelp;
        }
        List<String> filtered = new ArrayList<>();
        for (String line : LoadMessages.commandhelp) {
            if (!isAdminBarHelpLine(line)) {
                filtered.add(line);
            }
        }
        return filtered;
    }

    private static boolean isAdminBarHelpLine(String line) {
        if (line == null) {
            return false;
        }
        String lower = line.toLowerCase();
        return lower.contains("tpsbar")
                || lower.contains("rambar")
                || lower.contains("msptbar")
                || lower.contains("allbars");
    }
}
