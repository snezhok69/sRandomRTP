package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class onTabCompletes implements TabCompleter {

    public List<String> getAllArguments(CommandSender sender, String[] args) {
        try {
            List<String> arguments = new ArrayList<>();
            if (args.length == 1) {
                if (sender.hasPermission("sRandomRTP.command.Reload")) {
                    arguments.add("reload");
                }
                if (sender.hasPermission("sRandomRTP.Command.Cancel")) {
                    arguments.add("cancel");
                }
                if (sender.hasPermission("sRandomRTP.Command.Version")) {
                    arguments.add("version");
                }
                if (sender.hasPermission("sRandomRTP.Command.Near")) {
                    arguments.add("near");
                }
                if (sender.hasPermission("sRandomRTP.Command.Help")) {
                    arguments.add("help");
                }
                if (sender.hasPermission("sRandomRTP.Command.Player")) {
                    arguments.add("player");
                }
                if (sender.hasPermission("sRandomRTP.Command.Base")) {
                    arguments.add("base");
                }
                if (sender.hasPermission("sRandomRTP.Command.Back")) {
                    arguments.add("back");
                }
                if (sender.hasPermission("sRandomRTP.Command.World")) {
                    arguments.add("world");
                }
                if (sender.hasPermission("sRandomRTP.Command.Accept")) {
                    arguments.add("accept");
                }
                if (sender.hasPermission("sRandomRTP.Command.Deny")) {
                    arguments.add("deny");
                }
                if (sender.hasPermission("sRandomRTP.Command.SetPortal")) {
                    arguments.add("setportal");
                }
                if (sender.hasPermission("sRandomRTP.Command.DelPortal")) {
                    arguments.add("delportal");
                }
                if (sender.hasPermission("sRandomRTP.Command.ListPortal")) {
                    arguments.add("listportal");
                }
                if (sender.hasPermission("sRandomRTP.Command.ListPortal")) {
                    arguments.add("chunky");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("world") && sender.hasPermission("sRtp.Command.World")) {
                int count = 0;
                boolean isEnabled = Variables.teleportfile.getBoolean("teleport.bannedworld.enabled");
                List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                for (World world : Bukkit.getWorlds()) {
                    if (count >= 100) {
                        break;
                    }
                    if (isEnabled && bannedWorlds.contains(world.getName())) {
                        continue;
                    }
                    arguments.add(world.getName());
                    count++;
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                return playerNames;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("delportal")) {
                String partialPortalName = args[1];
                Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
                if (playerPortals != null) {
                    playerPortals.keySet().stream()
                            .filter(portalName -> portalName.toLowerCase().startsWith(partialPortalName))
                            .limit(8)
                            .forEach(arguments::add);
                    return arguments;
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("listportal")) {
                String partialPortalName = args[1];
                Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
                if (playerPortals != null) {
                    playerPortals.keySet().stream()
                            .filter(portalName -> portalName.toLowerCase().startsWith(partialPortalName))
                            .limit(8)
                            .forEach(arguments::add);
                    return arguments;
                }
            }
            return arguments;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                String input = args[0].toLowerCase();
                for (String argument : getAllArguments(sender, args)) {
                    if (argument.toLowerCase().startsWith(input)) {
                        completions.add(argument);
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("reload") ||
                    args[0].equalsIgnoreCase("cancel") ||
                    args[0].equalsIgnoreCase("near") ||
                    args[0].equalsIgnoreCase("world") ||
                    args[0].equalsIgnoreCase("help") ||
                    args[0].equalsIgnoreCase("player") ||
                    args[0].equalsIgnoreCase("base") ||
                    args[0].equalsIgnoreCase("back") ||
                    args[0].equalsIgnoreCase("accept") ||
                    args[0].equalsIgnoreCase("delportal") ||
                    args[0].equalsIgnoreCase("listportal") ||
                    args[0].equalsIgnoreCase("chunky") ||
                    args[0].equalsIgnoreCase("version"))) {
                String input = args[1].toLowerCase();
                for (String argument : getAllArguments(sender, args)) {
                    if (argument.toLowerCase().startsWith(input)) {
                        completions.add(argument);
                    }
                }
            }
            return completions;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return java.util.Collections.emptyList();
    }
}
