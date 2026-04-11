package org.sRandomRTP.DifferentMethods.Player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class CommandRun {

    public static void commandrun(Player player) {
        if (!Variables.teleportfile.getBoolean("teleport.Commandsteleport.enabled")) {
            return;
        }
        List<String> commandList = Variables.teleportfile.getStringList("teleport.Commandsteleport.Commands");
        // Sanitize player name to prevent command injection on servers with non-standard auth plugins
        String playerName = player.getName().replaceAll("[^a-zA-Z0-9_]", "");
        Variables.getFoliaLib().getImpl().runNextTick(task -> {
            for (String command : commandList) {
                String replacedCommand = command.replace("%player%", playerName);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacedCommand);
            }
        });
    }
}
