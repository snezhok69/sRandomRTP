package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandRun {
    public static void commandrun(Player player) {
        if (Variables.teleportfile.getBoolean("teleport.Commandsteleport.enabled")) {
            List<String> commandList = Variables.teleportfile.getStringList("teleport.Commandsteleport.Commands");
            for (String command : commandList) {
                String replacedCommand = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacedCommand);
            }
        }
    }
}
