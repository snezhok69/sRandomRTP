package org.sRandomRTP.Commands;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarPlayer;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.RequirementChecker;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class CommandPlayer {

    public static void commandPlayer(CommandSender sender, Player targetPlayer, World targetWorld) {
        try {
            if (targetPlayer == null) {
                Variables.sendError(sender, "Target player not found.");
                return;
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (targetPlayer.equals(sender)) {
                Variables.getMessageService().send(sender, LoadMessages.error_teleport_yourself);
                return;
            }
            if (!(sender instanceof Player)) {
                Variables.sendPlayersOnly(sender);
                return;
            }
            Player player = (Player) sender;
            World world = targetWorld != null ? targetWorld : targetPlayer.getWorld();
            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);

            if (!player.hasPermission(Permissions.PLAYER)) {
                Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
                return;
            }

            if (!CommandUtils.checkWorldGuard(sender, loggingEnabled)) return;

            int teleportCost = RequirementChecker.checkRequirements(player, targetPlayer, loggingEnabled);
            if (teleportCost < 0) return;

            if (state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getName(), false)) {
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
