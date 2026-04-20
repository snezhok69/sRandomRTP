package org.sRandomRTP.Rtp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Find.FindNearestPlayerNear;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.Files.LoadMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class RtpRtpNear {

    public static void rtpRtpNear(CommandSender sender) {
        new Handler().launchRtp(sender, null);
    }

    private static class Handler extends AbstractRtpHandler {
        private final AtomicReference<Player> targetPlayerRef = new AtomicReference<>();
        private int nearMinRadius;
        private int nearMaxRadius;

        @Override
        protected LaunchParams buildLaunchParams(Player player, World world, boolean loggingEnabled) {
            nearMinRadius = Variables.getPluginContext().getConfigRegistry().getNearFile().getInt("teleport.minRadius");
            nearMaxRadius = Variables.getPluginContext().getConfigRegistry().getNearFile().getInt("teleport.maxRadius");
            int maxAttempts = Math.max(1, Variables.getPluginContext().getConfigRegistry().getTeleportFile().getInt("teleport.maxtries"));

            List<Player> otherPlayers = new ArrayList<>(world.getPlayers());
            otherPlayers.remove(player);
            if (otherPlayers.isEmpty()) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                return null;
            }

            Player target = FindNearestPlayerNear.findRandomPlayer(player, otherPlayers);
            if (target == null) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                return null;
            }

            targetPlayerRef.set(target);
            // Pass nearMaxRadius as radius and nearMinRadius as minRadius.
            // generateXZ() ignores these and computes its own target-relative coordinates.
            // Passing distinct non-zero values avoids the radius==minRadius early-exit guard.
            return new LaunchParams(0, 0, nearMaxRadius, nearMinRadius, maxAttempts, false);
        }

        @Override
        protected boolean preAttemptChecks(Player player, World world, TeleportRequestContext ctx,
                                           boolean loggingEnabled, int attemptNumber,
                                           int maxAttempts, Runnable retryCallback) {
            Player target = targetPlayerRef.get();
            if (target != null && target.isOnline()) return true;

            List<Player> otherPlayers = new ArrayList<>(world.getPlayers());
            otherPlayers.remove(player);
            if (otherPlayers.isEmpty()) {
                Variables.getMessageService().send(player, LoadMessages.noplayerworldnear);
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no players nearby");
                Variables.getRuntimeState().setPlayerSearching(player, false);
                return false;
            }
            target = FindNearestPlayerNear.findRandomPlayer(player, otherPlayers);
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
            Location playerLoc = player.getLocation();
            if (playerLoc == null) return null;
            double distanceToTarget = playerLoc.distance(targetLoc);
            int radius = distanceToTarget <= nearMaxRadius ? nearMinRadius : nearMaxRadius;

            double randomAngle = Variables.getRngProvider().nextDouble(0.0D, Math.PI * 2);
            double randomRadius = Math.sqrt(Variables.getRngProvider().nextDouble()) * radius;
            int newX = (int) (targetLoc.getBlockX() + randomRadius * Math.cos(randomAngle));
            int newZ = (int) (targetLoc.getBlockZ() + randomRadius * Math.sin(randomAngle));

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
