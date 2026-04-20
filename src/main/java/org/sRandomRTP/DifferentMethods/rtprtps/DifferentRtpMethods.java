package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ConfigUtils;

import java.util.concurrent.CompletableFuture;

import org.sRandomRTP.Utils.ChatUtils;

public class DifferentRtpMethods {

    public static boolean isHazardousFluid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    public static boolean shouldLogAttempt(int attemptNumber) {
        return attemptNumber == 1 || attemptNumber % 5 == 0;
    }

    public static void handleTimedOutSearch(Player player, boolean loggingEnabled) {
        player.sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cTeleportation request timed out. Please try again.");
        if (loggingEnabled) {
            Bukkit.getLogger().warning("Teleportation search timed out for player " + player.getName());
        }
    }

    public static final class ClampedRadius {
        public final int radius;
        public final int minRadius;
        public ClampedRadius(int radius, int minRadius) {
            this.radius = radius;
            this.minRadius = minRadius;
        }
    }

    public static ClampedRadius clampRadiusToBorder(World world, int radius, int minRadius,
                                                     String logPrefix, boolean loggingEnabled) {
        WorldBorder worldBorder = world.getWorldBorder();
        double borderSize = worldBorder.getSize() / 2.0;
        int maxPossibleRadius = (int) Math.floor(borderSize * 0.95);

        boolean radiusAdjusted = false;
        if (radius > maxPossibleRadius) {
            if (loggingEnabled) Bukkit.getConsoleSender().sendMessage(
                    logPrefix + " Radius " + radius + " exceeds world border size (" + (int) borderSize + "). Adjusting to " + maxPossibleRadius);
            radius = maxPossibleRadius;
            radiusAdjusted = true;
        }
        if (minRadius > maxPossibleRadius) { minRadius = Math.max(0, maxPossibleRadius - 100); radiusAdjusted = true; }
        if (minRadius >= radius)           { minRadius = Math.max(0, radius - 100);            radiusAdjusted = true; }
        if (radiusAdjusted && loggingEnabled) Bukkit.getConsoleSender().sendMessage(
                logPrefix + " Final adjusted values for world '" + world.getName() + "': radius=" + radius + ", minRadius=" + minRadius);
        if (loggingEnabled) Bukkit.getConsoleSender().sendMessage(
                logPrefix + " radius=" + radius + ", minRadius=" + minRadius);
        return new ClampedRadius(radius, minRadius);
    }

}
