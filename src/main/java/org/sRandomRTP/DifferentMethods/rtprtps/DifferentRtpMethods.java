package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Rtp.RtpRtp;

import java.util.concurrent.CompletableFuture;

import static org.sRandomRTP.DifferentMethods.Variables.pluginName;

public class DifferentRtpMethods {

    public static boolean isHazardousFluid(Material material) {
        return material == Material.WATER || material == Material.LAVA;
    }

    public static boolean shouldLogAttempt(int attemptNumber) {
        return attemptNumber == 1 || attemptNumber % 5 == 0;
    }

    public static void handleTimedOutSearch(Player player, boolean loggingEnabled) {
        player.sendMessage(pluginName + " §8- §cTeleportation request timed out. Please try again.");
        if (loggingEnabled) {
            Bukkit.getLogger().warning("Teleportation search timed out for player " + player.getName());
        }
    }

    public static int getWorldSpecificRadius(String worldName, String setting) {
        String worldPath = "teleport.per-world." + worldName + "." + setting;
        if (Variables.teleportfile.contains(worldPath)) {
            return Variables.teleportfile.getInt(worldPath);
        }

        return Variables.teleportfile.getInt("teleport." + setting);
    }
}
