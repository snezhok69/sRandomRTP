package org.sRandomRTP.Commands;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalSQLRepository;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class CommandDelPortal {

    public static void commandDelPortal(CommandSender sender, String portalName) {
        if (!CommandUtils.requirePlayer(sender).isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.PORTAL)) return;
        RuntimeStateRegistry state = Variables.getRuntimeState();
        PortalData portalData = state.getPlayerPortal(sender.getName(), portalName);
        if (portalData == null) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_not_found,
                    "%portal%", portalName);
            return;
        }

        state.removePlayerPortal(sender.getName(), portalName);
        PortalSQLRepository.removePortalPlayerFromDatabaseSQL(sender.getName(), portalName).join();
        PortalSQLRepository.removePortalBlocksPlayerToDatabaseSQL(portalName);
        PortalSQLRepository.removePortalTasksFromDatabaseSQL(portalName);

        Variables.getMessageService().send(sender, LoadMessages.portal_delete_success,
                "%portal%", portalName);
    }
}
