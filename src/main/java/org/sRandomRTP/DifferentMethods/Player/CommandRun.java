package org.sRandomRTP.DifferentMethods.Player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;
import java.util.regex.Pattern;

public class CommandRun {

    /** Pre-compiled pattern — compiling regex on every call is wasteful. */
    private static final Pattern PLAYER_NAME_SANITIZE = Pattern.compile("[^a-zA-Z0-9_]");

    public static void commandrun(Player player) {
        org.bukkit.configuration.file.FileConfiguration teleportfile = Variables.getPluginContext().getConfigRegistry().getTeleportFile();
        if (!teleportfile.getBoolean("teleport.Commandsteleport.enabled")) {
            return;
        }
        List<String> commandList = teleportfile.getStringList("teleport.Commandsteleport.Commands");
        // Sanitize player name to prevent command injection on servers with non-standard auth plugins
        String playerName = PLAYER_NAME_SANITIZE.matcher(player.getName()).replaceAll("");
        Variables.getFoliaLib().getImpl().runNextTick(task -> {
            for (String command : commandList) {
                String replacedCommand = command.replace("%player%", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacedCommand);
            }
        });
    }
}
