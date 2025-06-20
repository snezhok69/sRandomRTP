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
                if (sender.hasPermission("sRandomRTP.Command.Portal")) {
                    arguments.add("portal");
                }
                if (sender.hasPermission("sRandomRTP.Command.Chunky")) {
                    arguments.add("chunky");
                }
                if (sender.hasPermission("sRandomRTP.Command.Far")) {
                    arguments.add("far");
                }
                if (sender.hasPermission("sRandomRTP.Command.Middle")) {
                    arguments.add("middle");
                }
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("portal") && sender.hasPermission("sRandomRTP.Command.Portal")) {
                    arguments.add("set");
                    arguments.add("del");
                    arguments.add("list");
                } else if (args[0].equalsIgnoreCase("world") && sender.hasPermission("sRandomRTP.Command.World")) {
                    int count = 0;
                    boolean isEnabled = Variables.teleportfile.getBoolean("teleport.bannedworld.enabled");
                    List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                    for (World world : Bukkit.getWorlds()) {
                        if (count >= 10) {
                            break;
                        }
                        if (isEnabled && bannedWorlds.contains(world.getName())) {
                            continue;
                        }
                        arguments.add(world.getName());
                        count++;
                    }
                } else if (args[0].equalsIgnoreCase("player")) {
                    List<String> playerNames = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        playerNames.add(player.getName());
                    }
                    return playerNames;
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("portal")) {
                    if (args[1].equalsIgnoreCase("del") || args[1].equalsIgnoreCase("list")) {
                        String partialPortalName = args[2].toLowerCase();
                        Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
                        if (playerPortals != null) {
                            playerPortals.keySet().stream()
                                    .filter(portalName -> portalName.toLowerCase().startsWith(partialPortalName))
                                    .limit(8)
                                    .forEach(arguments::add);
                        }
                    }
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
            } else if (args.length == 2) {
                String input = args[1].toLowerCase();
                if (args[0].equalsIgnoreCase("player")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().toLowerCase().startsWith(input)) {
                            completions.add(player.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("portal") && sender.hasPermission("sRandomRTP.Command.Portal")) {
                    if ("set".startsWith(input)) completions.add("set");
                    if ("del".startsWith(input)) completions.add("del");
                    if ("list".startsWith(input)) completions.add("list");
                } else if (args[0].equalsIgnoreCase("chunky")) {
                    if ("stop".startsWith(input)) completions.add("stop");
                } else if (args[0].equalsIgnoreCase("world")) {
                    boolean isEnabled = Variables.teleportfile.getBoolean("teleport.bannedworld.enabled");
                    List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                    for (World world : Bukkit.getWorlds()) {
                        if (isEnabled && bannedWorlds.contains(world.getName())) {
                            continue;
                        }
                        if (world.getName().toLowerCase().startsWith(input)) {
                            completions.add(world.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("reload") ||
                        args[0].equalsIgnoreCase("cancel") ||
                        args[0].equalsIgnoreCase("near") ||
                        args[0].equalsIgnoreCase("help") ||
                        args[0].equalsIgnoreCase("base") ||
                        args[0].equalsIgnoreCase("back") ||
                        args[0].equalsIgnoreCase("accept") ||
                        args[0].equalsIgnoreCase("deny") ||
                        args[0].equalsIgnoreCase("chunky") ||
                        args[0].equalsIgnoreCase("far") ||
                        args[0].equalsIgnoreCase("middle") ||
                        args[0].equalsIgnoreCase("version")) {
                    for (String argument : getAllArguments(sender, args)) {
                        if (argument.toLowerCase().startsWith(input)) {
                            completions.add(argument);
                        }
                    }
                }
            } else if (args.length == 3) {
                String input = args[2].toLowerCase();
                if (args[0].equalsIgnoreCase("portal")) {
                    if (args[1].equalsIgnoreCase("del") || args[1].equalsIgnoreCase("list")) {
                        Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
                        if (playerPortals != null) {
                            playerPortals.keySet().stream()
                                    .filter(portalName -> portalName.toLowerCase().startsWith(input))
                                    .limit(8)
                                    .forEach(completions::add);
                        }
                    }
                } else if (args[0].equalsIgnoreCase("player")) {
                    boolean isEnabled = Variables.teleportfile.getBoolean("teleport.bannedworld.enabled");
                    List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                    for (World world : Bukkit.getWorlds()) {
                        if (isEnabled && bannedWorlds.contains(world.getName())) {
                            continue;
                        }
                        if (world.getName().toLowerCase().startsWith(input)) {
                            completions.add(world.getName());
                        }
                    }
                } else if (args[0].equalsIgnoreCase("chunky") && !args[1].equalsIgnoreCase("stop")) {
                    if ("stop".startsWith(input)) completions.add("stop");
                }
            } else if (args.length == 5) {
                String input = args[4].toLowerCase();
                if (args[0].equalsIgnoreCase("portal") && args[1].equalsIgnoreCase("set")) {
                    if ("circle".startsWith(input)) completions.add("circle");
                    if ("square".startsWith(input)) completions.add("square");
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
