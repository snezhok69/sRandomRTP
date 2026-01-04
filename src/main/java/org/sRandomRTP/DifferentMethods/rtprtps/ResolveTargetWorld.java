package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class ResolveTargetWorld {
    public static World resolveTargetWorld(Player player, World targetWorld, boolean loggingEnabled) {
        if (targetWorld != null) {
            return targetWorld;
        }

        World currentWorld = player.getWorld();
        if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
            List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
            if (bannedWorlds.contains(currentWorld.getName())) {
                if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                    String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                    World redirectWorld = Bukkit.getWorld(redirectWorldName);
                    if (redirectWorld != null) {
                        return redirectWorld;
                    }
                    if (loggingEnabled) {
                        Bukkit.getLogger().warning("Redirect world " + redirectWorldName + " is not loaded. Using player's current world.");
                    }
                }
                return currentWorld;
            }
        }
        return currentWorld;
    }
}
