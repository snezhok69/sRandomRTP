package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalSQLRepository;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.concurrent.CompletableFuture;

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

        // All three DB operations run concurrently; memory is only updated after all complete.
        // This prevents an out-of-sync state if the server crashes between DB operations.
        CompletableFuture.allOf(
                PortalSQLRepository.removePortalPlayerFromDatabaseSQL(sender.getName(), portalName),
                PortalSQLRepository.removePortalBlocksPlayerToDatabaseSQL(portalName),
                PortalSQLRepository.removePortalTasksFromDatabaseSQL(portalName)
        ).thenRun(() -> {
            state.removePlayerPortal(sender.getName(), portalName);
            Variables.getFoliaLib().getImpl().runAtEntity(
                    (Player) sender,
                    t -> Variables.getMessageService().send(sender, LoadMessages.portal_delete_success,
                            "%portal%", portalName));
        }).exceptionally(ex -> {
            ChatUtils.logError("Portal deletion failed for '" + portalName + "': " + ex.getMessage());
            return null;
        });
    }
}
