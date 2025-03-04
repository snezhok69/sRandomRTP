package org.sRandomRTP.Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.RemovePortalPlayerFromDatabaseSQL;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.*;

public class CommandDelPortal {

    public static void commandDelPortal(CommandSender sender, String portalName) {
        if (!(sender instanceof Player)) {
            for (String line : LoadMessages.nopermissioncommand) {
                sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
            }
            return;
        }
        //
        Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
        if (playerPortals == null || !playerPortals.containsKey(portalName)) {
            for (String line : LoadMessages.error_portal_not_found) {
                sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%portal%", portalName))));
            }
            return;
        }
        //
        playerPortals.remove(portalName);
        RemovePortalPlayerFromDatabaseSQL.removePortalPlayerFromDatabaseSQL(sender.getName(), portalName).join();
        RemovePortalPlayerFromDatabaseSQL.removePortalBlocksPlayerToDatabaseSQL(portalName);
        RemovePortalPlayerFromDatabaseSQL.removePortalTasksFromDatabaseSQL(portalName);
        //
        for (String line : LoadMessages.portal_delete_success) {
            sender.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%portal%", portalName))));
        }
    }
}