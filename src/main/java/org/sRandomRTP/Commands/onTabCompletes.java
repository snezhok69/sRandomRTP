package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnTabCompletes implements TabCompleter {

    public List<String> getAllArguments(CommandSender sender, String[] args) {
        try {
            if (args.length == 1) {
                return getRootArguments(sender);
            }
            if (args.length == 2) {
                return getSecondArgumentSuggestions(sender, args);
            }
            if ("biome".equalsIgnoreCase(args[0]) && args.length >= 3) {
                return getBiomeArgumentSuggestions(args[args.length - 1]);
            }
            if (args.length == 3) {
                return getThirdArgumentSuggestions(sender, args);
            }
            if (args.length == 5 && isPortalSetShapeRequest(args)) {
                return PortalCommandSupport.shapeSuggestions();
            }
            return java.util.Collections.emptyList();
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(OnTabCompletes.class, e);
        }
        return java.util.Collections.emptyList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            List<String> completions = new ArrayList<>();
            String input = args.length == 0 ? "" : args[args.length - 1].toLowerCase();
            for (String argument : getAllArguments(sender, args)) {
                if (argument != null && argument.toLowerCase().startsWith(input)) {
                    completions.add(argument);
                }
            }
            return completions;
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(OnTabCompletes.class, e);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> getRootArguments(CommandSender sender) {
        List<String> arguments = new ArrayList<String>();
        if (sender.hasPermission(Permissions.RELOAD)) {
            arguments.add("reload");
        }
        if (sender.hasPermission(Permissions.CANCEL)) {
            arguments.add("cancel");
        }
        if (sender.hasPermission(Permissions.VERSION)) {
            arguments.add("version");
        }
        if (sender.hasPermission(Permissions.NEAR)) {
            arguments.add("near");
        }
        if (sender.hasPermission(Permissions.HELP)) {
            arguments.add("help");
        }
        if (sender.hasPermission(Permissions.PLAYER)) {
            arguments.add("player");
        }
        if (sender.hasPermission(Permissions.BASE)) {
            arguments.add("base");
        }
        if (sender.hasPermission(Permissions.BACK)) {
            arguments.add("back");
        }
        if (sender.hasPermission(Permissions.WORLD)) {
            arguments.add("world");
        }
        if (sender.hasPermission(Permissions.ACCEPT)) {
            arguments.add("accept");
        }
        if (sender.hasPermission(Permissions.DENY)) {
            arguments.add("deny");
        }
        if (sender.hasPermission(Permissions.PORTAL)) {
            arguments.add("portal");
        }
        if (sender.hasPermission(Permissions.CHUNKY)) {
            arguments.add("chunky");
        }
        if (sender.hasPermission(Permissions.FAR)) {
            arguments.add("far");
        }
        if (sender.hasPermission(Permissions.MIDDLE)) {
            arguments.add("middle");
        }
        if (sender.hasPermission(Permissions.RTP_BIOME)) {
            arguments.add("biome");
        }
        return arguments;
    }

    private List<String> getSecondArgumentSuggestions(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();
        if ("portal".equals(subCommand) && sender.hasPermission(Permissions.PORTAL)) {
            return PortalCommandSupport.actionSuggestions();
        }
        if ("world".equals(subCommand) && sender.hasPermission(Permissions.WORLD)) {
            return getWorldSuggestions(10);
        }
        if ("player".equals(subCommand)) {
            return getOnlinePlayerNames();
        }
        if ("biome".equals(subCommand) && sender.hasPermission(Permissions.RTP_BIOME)) {
            return getBiomeArgumentSuggestions(args[1]);
        }
        if ("chunky".equals(subCommand)) {
            return Arrays.asList("stop");
        }
        return java.util.Collections.emptyList();
    }

    private List<String> getThirdArgumentSuggestions(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();
        if ("portal".equals(subCommand)
                && ("del".equalsIgnoreCase(args[1]) || "list".equalsIgnoreCase(args[1]))) {
            return PortalCommandSupport.portalNameSuggestions(sender, args[2], 8);
        }
        if ("player".equals(subCommand)) {
            return getWorldSuggestions(10);
        }
        if ("chunky".equals(subCommand) && !"stop".equalsIgnoreCase(args[1])) {
            return Arrays.asList("stop");
        }
        if ("biome".equals(subCommand) && sender.hasPermission(Permissions.RTP_BIOME)) {
            return getSimpleBiomeSuggestions(args[2]);
        }
        return java.util.Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames() {
        List<String> playerNames = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    private List<String> getWorldSuggestions(int limit) {
        List<String> worlds = new ArrayList<String>();
        org.bukkit.configuration.file.FileConfiguration teleportfile = Variables.getPluginContext().getConfigRegistry().getTeleportFile();
        boolean isEnabled = teleportfile.getBoolean("teleport.bannedworld.enabled");
        List<String> bannedWorlds = teleportfile.getStringList("teleport.bannedworld.worlds");
        for (World world : Bukkit.getWorlds()) {
            if (isEnabled && bannedWorlds.contains(world.getName())) {
                continue;
            }
            worlds.add(world.getName());
            if (limit > 0 && worlds.size() >= limit) {
                break;
            }
        }
        return worlds;
    }

    private boolean isPortalSetShapeRequest(String[] args) {
        return args.length == 5
                && "portal".equalsIgnoreCase(args[0])
                && "set".equalsIgnoreCase(args[1]);
    }

    private List<String> getBiomeArgumentSuggestions(String rawInput) {
        String value = rawInput == null ? "" : rawInput;
        int commaIndex = value.lastIndexOf(',');
        int tokenStart = commaIndex >= 0 ? commaIndex + 1 : 0;
        while (tokenStart < value.length() && Character.isWhitespace(value.charAt(tokenStart))) {
            tokenStart++;
        }
        String prefix = value.substring(0, Math.min(tokenStart, value.length()));
        if (commaIndex >= 0 && tokenStart == commaIndex + 1) {
            prefix = prefix + " ";
        }
        String query = tokenStart <= value.length() ? value.substring(tokenStart) : "";
        return collectBiomeSuggestions(prefix, query);
    }

    private List<String> getSimpleBiomeSuggestions(String input) {
        return collectBiomeSuggestions("", input == null ? "" : input);
    }

    private List<String> collectBiomeSuggestions(String prefix, String query) {
        List<String> suggestions = new ArrayList<>();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        boolean enforceLimit = normalized.isEmpty();
        for (Biome biome : Biome.values()) {
            String name = biome.name();
            if (normalized.isEmpty() || name.toLowerCase().startsWith(normalized)) {
                suggestions.add(prefix + name);
                if (enforceLimit && suggestions.size() >= 10) {
                    break;
                }
            }
        }
        return suggestions;
    }
}
