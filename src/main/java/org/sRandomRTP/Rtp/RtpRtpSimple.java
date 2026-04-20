package org.sRandomRTP.Rtp;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;
import org.sRandomRTP.Utils.ConfigUtils;
import org.sRandomRTP.Utils.CoordinateUtils;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.clampRadiusToBorder;

/**
 * Universal single-world RTP handler.
 * Replaces the duplicated inner Handler classes from RtpRtpPlayer and RtpRtpPortal.
 *
 * @param useWorldSpecificRadius {@code true} — reads per-world radius from config,
 *                               {@code false} — uses global teleport.radius.
 */
public final class RtpRtpSimple {

    private RtpRtpSimple() {}

    /**
     * Starts an RTP search for the player in the given world.
     *
     * @param player                 player to teleport
     * @param targetWorld            target world
     * @param useWorldSpecificRadius {@code true} — per-world radius, {@code false} — global
     * @param logPrefix              prefix for radius error messages
     */
    public static void launch(Player player, World targetWorld,
                              boolean useWorldSpecificRadius, String logPrefix) {
        new AbstractRtpHandler() {
            @Override
            protected LaunchParams buildLaunchParams(Player p, World world, boolean loggingEnabled) {
                int centerX = worldCenterX(world);
                int centerZ = worldCenterZ(world);

                int radius;
                int minRadius;
                if (useWorldSpecificRadius) {
                    String worldName = world.getName();
                    org.bukkit.configuration.file.FileConfiguration teleportFile =
                            Variables.getPluginContext().getConfigRegistry().getTeleportFile();
                    // Inlined from the former DifferentRtpMethods.getWorldSpecificRadius wrapper
                    radius    = ConfigUtils.getWorldSpecificInt(teleportFile, worldName, "radius",    0);
                    minRadius = ConfigUtils.getWorldSpecificInt(teleportFile, worldName, "minradius", 0);
                } else {
                    // Use ConfigCache snapshot — avoids live YAML lookups on every RTP
                    radius    = Variables.configCache.maxRadius;
                    minRadius = Variables.configCache.minRadius;
                }

                DifferentRtpMethods.ClampedRadius clamped =
                        clampRadiusToBorder(world, radius, minRadius, logPrefix, loggingEnabled);
                radius    = clamped.radius;
                minRadius = clamped.minRadius;

                if (!validateRadius(minRadius, radius, p)) return null;

                int maxAttempts = Variables.configCache.maxTries;
                return new LaunchParams(centerX, centerZ, radius, minRadius, maxAttempts, true);
            }
        }.launchRtpForPlayer(player, targetWorld);
    }
}
