package org.sRandomRTP.Rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Find.FindNearestPlayerNear;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.bukkit.ChatColor.translateAlternateColorCodes;
import static org.sRandomRTP.DifferentMethods.Variables.pluginName;
import static org.sRandomRTP.DifferentMethods.rtprtps.HandleFailedAttempt.handleFailedAttempt;
import static org.sRandomRTP.DifferentMethods.rtprtps.SendLoadingFeedback.sendLoadingFeedback;

public class RtpRtpNear {
    public static void rtpRtpNear(CommandSender sender) {
        Player player = sender instanceof Player ? (Player) sender : null;
        boolean searchStarted = false;
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (!(sender instanceof Player)) {
                Variables.sendPlayersOnly(sender);
                return;
            }

            player = (Player) sender;

            if (!player.isOnline()) return;

            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);

            World world = player.getWorld();

            sendLoadingFeedback(player);
            state.rememberInitialPosition(player);

            TeleportRequestContext context = TeleportRequestManager.beginRequest(player, loggingEnabled);
            searchStarted = true;

            state.setPlayerSearching(player, true);

            int maxAttempts = Math.max(1, Variables.teleportfile.getInt("teleport.maxtries"));
            int minRadius = Variables.nearfile.getInt("teleport.minRadius");
            int maxRadius = Variables.nearfile.getInt("teleport.maxRadius");

            List<Player> otherPlayers = new ArrayList<>(world.getPlayers());
            otherPlayers.remove(player);
            if (otherPlayers.isEmpty()) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no players nearby");
                state.setPlayerSearching(player, false);
                return;
            }

            Player targetPlayer = FindNearestPlayerNear.findNearestPlayer(player, otherPlayers);
            if (targetPlayer == null) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no valid target player");
                state.setPlayerSearching(player, false);
                return;
            }

            AtomicReference<Player> targetPlayerRef = new AtomicReference<>(targetPlayer);
            Handler handler = new Handler(targetPlayerRef, minRadius, maxRadius);

            // Pass maxRadius as radius and minRadius as minRadius; generateXZ ignores these and uses its own logic.
            // We pass distinct values (maxRadius != 0) to avoid the radius==minRadius early-exit guard.
            handler.scheduleNextAttempt(player, world, 0, 0, maxRadius, 0,
                    maxAttempts, loggingEnabled, config, context, 0L, 0L);
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(RtpRtpNear.class, e);
        } finally {
            if (!searchStarted && player != null) EconomyPaymentManager.refund(player);
        }
    }

    private static class Handler extends AbstractRtpHandler {
        private final AtomicReference<Player> targetPlayerRef;
        private final int nearMinRadius;
        private final int nearMaxRadius;

        Handler(AtomicReference<Player> targetPlayerRef, int nearMinRadius, int nearMaxRadius) {
            this.targetPlayerRef = targetPlayerRef;
            this.nearMinRadius = nearMinRadius;
            this.nearMaxRadius = nearMaxRadius;
        }

        @Override
        protected boolean preAttemptChecks(Player player, World world, TeleportRequestContext ctx,
                                           boolean loggingEnabled, int attemptNumber,
                                           int maxAttempts, Runnable retryCallback) {
            Player target = targetPlayerRef.get();
            if (target != null && target.isOnline()) return true;

            // Re-find nearest player
            List<Player> otherPlayersRetry = new ArrayList<>(world.getPlayers());
            otherPlayersRetry.remove(player);
            if (otherPlayersRetry.isEmpty()) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no players nearby");
                Variables.getRuntimeState().setPlayerSearching(player, false);
                return false;
            }
            target = FindNearestPlayerNear.findNearestPlayer(player, otherPlayersRetry);
            if (target == null) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no valid target player");
                Variables.getRuntimeState().setPlayerSearching(player, false);
                return false;
            }
            targetPlayerRef.set(target);
            return true;
        }

        @Override
        protected int[] generateXZ(Player player, World world, int ignoredCenterX, int ignoredCenterZ,
                                   int ignoredRadius, int ignoredMinRadius, int generationIndex,
                                   long sessionNonce, String method, boolean absolute,
                                   boolean loggingEnabled, int attemptNumber) {
            Player target = targetPlayerRef.get();
            if (target == null || !target.isOnline()) return null;

            Location targetLoc = target.getLocation();
            if (targetLoc == null) return null;
            double distanceToTarget = player.getLocation().distance(targetLoc);
            int radius = distanceToTarget <= nearMaxRadius ? nearMinRadius : nearMaxRadius;

            double randomAngle = Variables.getRngProvider().nextDouble(0.0D, Math.PI * 2);
            double randomRadius = Math.sqrt(Variables.getRngProvider().nextDouble()) * radius;
            int newX = (int) (targetLoc.getBlockX() + randomRadius * Math.cos(randomAngle));
            int newZ = (int) (targetLoc.getBlockZ() + randomRadius * Math.sin(randomAngle));

            // Reject coordinates outside the world border early to avoid wasting a chunk load
            org.bukkit.WorldBorder border = world.getWorldBorder();
            if (border != null && !border.isInside(new Location(world, newX + 0.5, 64, newZ + 0.5))) {
                return null;
            }

            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Generating coordinates near player " + target.getName()
                        + " at distance " + radius + ": X=" + newX + ", Z=" + newZ);
            }

            return new int[]{newX, newZ};
        }
    }
}
