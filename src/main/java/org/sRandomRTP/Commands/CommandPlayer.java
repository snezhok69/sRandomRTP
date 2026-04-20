package org.sRandomRTP.Commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarPlayer;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.RequirementChecker;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class CommandPlayer {

    public static void commandPlayer(CommandSender sender, Player targetPlayer, World targetWorld) {
        try {
            if (targetPlayer == null) {
                ChatUtils.sendError(sender, "Target player not found.");
                return;
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (targetPlayer.equals(sender)) {
                Variables.getMessageService().send(sender, LoadMessages.error_teleport_yourself);
                return;
            }
            if (!(sender instanceof Player)) {
                ChatUtils.sendPlayersOnly(sender);
                return;
            }
            Player player = (Player) sender;
            World world = targetWorld != null ? targetWorld : targetPlayer.getWorld();
            boolean loggingEnabled = Variables.isLoggingEnabled();

            if (!player.hasPermission(Permissions.PLAYER)) {
                Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
                return;
            }

            if (!CommandUtils.checkWorldGuard(sender, loggingEnabled)) return;

            int teleportCost = RequirementChecker.checkRequirements(player, targetPlayer, loggingEnabled);
            if (teleportCost < 0) return;

            if (state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getUniqueId(), false)) {
                Variables.getMessageService().send(sender, LoadMessages.rtpplayeralreadyrequested);
                return;
            }

            Variables.getTeleportService().dispatchTargetTeleport(
                    sender,
                    player,
                    targetPlayer,
                    teleportCost,
                    () -> CooldownBypassBossBarPlayer.cooldownBypassBossBarplayer(sender, targetPlayer, world)
            );
        } catch (RuntimeException e) {
            org.sRandomRTP.DifferentMethods.LoggerUtility.loggerUtility(CommandPlayer.class, e);
        }
    }
}
