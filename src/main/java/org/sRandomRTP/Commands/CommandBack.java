package org.sRandomRTP.Commands;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.sRandomRTP.DifferentMethods.Teleport.CompatibleTeleport;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class CommandBack {
    public static void handleBackCommand(CommandSender sender) {
        java.util.Optional<Player> playerOpt = CommandUtils.requirePlayer(sender);
        if (!playerOpt.isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.BACK)) return;
        Player player = playerOpt.get();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        Location initialLocation = state.getInitialPosition(player);
        if (initialLocation != null) {
            CompatibleTeleport.teleport(
                    player,
                    initialLocation,
                    PlayerTeleportEvent.TeleportCause.PLUGIN,
                    Variables.isLoggingEnabled(),
                    "back command"
            ).whenComplete((success, throwable) -> FoliaSchedulerFacade.runAtEntity(player, () -> {
                if (throwable != null || !Boolean.TRUE.equals(success)) {
                    Variables.getMessageService().send(sender,
                            java.util.Collections.singletonList("&cTeleport failed. Check LogsErrors/latest-error.log"));
                    return;
                }
                Variables.getMessageService().send(sender, LoadMessages.teleportBackSuccess);
                state.removeInitialPosition(player);
            }));
        } else {
            Variables.getMessageService().send(sender, LoadMessages.teleportBackFailure);
        }
    }
}
