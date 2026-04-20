package org.sRandomRTP.Cooldowns;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.Permissions;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpSimple;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class CooldownBypassBossBarPlayer {

    /** Ticks to wait for the target player to accept/deny a teleport request (200 ticks = 10 seconds). */
    private static final long CONFIRM_TIMEOUT_TICKS = 200L;

    public static boolean cooldownBypassBossBarplayer(CommandSender sender, Player targetPlayer, World world) {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (targetPlayer.hasPermission(Permissions.COOLDOWN_BYPASS)) {
                RtpRtpSimple.launch(targetPlayer, world, true, "[sRandomRTP]");
                return true;
            }

            if (state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getUniqueId(), false)) {
                Variables.getMessageService().send(sender, LoadMessages.rtpplayeralreadyrequested);
                return false;
            }

            state.getPlayerConfirmStatus().put(targetPlayer.getUniqueId(), true);

            Variables.getMessageService().send(sender, LoadMessages.rtpplayerteleportrequestsent,
                    "%player%", targetPlayer.getName());

            if (Variables.configCache.rtpPlayerMessages) {
                state.getCommandSenderMap().put(targetPlayer.getUniqueId(), sender);
                sendInitialMessage(sender, targetPlayer, state);
            } else {
                startBossBarCountdown(sender, targetPlayer, world);
            }

            return true;
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(CooldownBypassBossBarPlayer.class, e);
            return false;
        }
    }

    private static void sendInitialMessage(CommandSender sender, Player targetPlayer, RuntimeStateRegistry state) {
        Variables.getMessageService().send(targetPlayer, LoadMessages.rtpplayerrequestinitiator,
                "%initiator%", sender.getName());

        Variables.getFoliaLib().getImpl().runLaterAsync(() -> {
            if (state.getPlayerConfirmStatus().getOrDefault(targetPlayer.getUniqueId(), false)) {
                Variables.getMessageService().send(targetPlayer, LoadMessages.rtpplayertimeout);
                state.clearPendingPlayerRouting(targetPlayer.getUniqueId());
            }
            state.getPlayerConfirmStatus().put(targetPlayer.getUniqueId(), false);
        }, CONFIRM_TIMEOUT_TICKS);
    }

    public static void denyTeleport(CommandSender sender, Player targetPlayer) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        java.util.UUID playerId = targetPlayer.getUniqueId();
        if (state.getPlayerConfirmStatus().getOrDefault(playerId, false)) {
            state.getPlayerConfirmStatus().put(playerId, false);
            Variables.getMessageService().send(targetPlayer, LoadMessages.rtpplayercanceled);
            CommandSender originalSender = state.getCommandSenderMap().get(playerId);
            if (originalSender != null) {
                Variables.getMessageService().send(originalSender, LoadMessages.rtpplayersendernotified,
                        "%target-player%", targetPlayer.getName());
                state.getCommandSenderMap().remove(playerId);
            }
        } else {
            Variables.getMessageService().send(targetPlayer, LoadMessages.rtpplayernoactiveteleport);
        }
    }

    public static void startBossBarCountdown(CommandSender sender, Player targetPlayer, World world) {
        BossBarCountdownEngine.startCountdown(targetPlayer, sender,
                () -> RtpRtpSimple.launch(targetPlayer, world, true, "[sRandomRTP]"));
    }
}
