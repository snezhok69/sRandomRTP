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
import org.sRandomRTP.Services.AdminBarService;
import org.sRandomRTP.Services.AdminBarType;
import org.sRandomRTP.Services.LocalFeatureGate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OnTabCompletes implements TabCompleter {
    private static final List<String> BIOME_NAMES = buildBiomeNames();

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
            if (args.length == 4
                    && "settings".equalsIgnoreCase(args[0])
                    && "toggle".equalsIgnoreCase(args[1])
                    && CommandFeatureFlag.SETTINGS.isVisibleTo(sender)) {
                return Arrays.asList("on", "off");
            }
            if (args.length == 5 && isPortalSetShapeRequest(args) && CommandFeatureFlag.PORTAL.isVisibleTo(sender)) {
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
        if (CommandFeatureFlag.RELOAD.isVisibleTo(sender)) {
            arguments.add("reload");
        }
        if (CommandFeatureFlag.CANCEL.isVisibleTo(sender)) {
            arguments.add("cancel");
        }
        if (CommandFeatureFlag.VERSION.isVisibleTo(sender)) {
            arguments.add("version");
        }
        if (CommandFeatureFlag.NEAR.isVisibleTo(sender)) {
            arguments.add("near");
        }
        if (CommandFeatureFlag.HELP.isVisibleTo(sender)) {
            arguments.add("help");
        }
        if (CommandFeatureFlag.PLAYER.isVisibleTo(sender)) {
            arguments.add("player");
        }
        if (CommandFeatureFlag.BASE.isVisibleTo(sender)) {
            arguments.add("base");
        }
        if (CommandFeatureFlag.BACK.isVisibleTo(sender)) {
            arguments.add("back");
        }
        if (CommandFeatureFlag.WORLD.isVisibleTo(sender)) {
            arguments.add("world");
        }
        if (CommandFeatureFlag.ACCEPT.isVisibleTo(sender)) {
            arguments.add("accept");
        }
        if (CommandFeatureFlag.DENY.isVisibleTo(sender)) {
            arguments.add("deny");
        }
        if (CommandFeatureFlag.SETTINGS.isVisibleTo(sender)) {
            arguments.add("settings");
        }
        if (CommandFeatureFlag.DOCTOR.isVisibleTo(sender)) {
            arguments.add("doctor");
        }
        if (CommandFeatureFlag.DUMP.isVisibleTo(sender)) {
            arguments.add("dump");
        }
        if (CommandFeatureFlag.STATS.isVisibleTo(sender)) {
            arguments.add("stats");
        }
        if (CommandFeatureFlag.PORTAL.isVisibleTo(sender) || CommandFeatureFlag.PORTAL_CHECK.isVisibleTo(sender)) {
            arguments.add("portal");
        }
        if (CommandFeatureFlag.CHUNKY.isVisibleTo(sender)) {
            arguments.add("chunky");
        }
        if (CommandFeatureFlag.FAR.isVisibleTo(sender)) {
            arguments.add("far");
        }
        if (CommandFeatureFlag.MIDDLE.isVisibleTo(sender)) {
            arguments.add("middle");
        }
        if (CommandFeatureFlag.BIOME.isVisibleTo(sender)) {
            arguments.add("biome");
        }
        addAdminBarArguments(sender, arguments);
        return arguments;
    }

    private List<String> getSecondArgumentSuggestions(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();
        if ("portal".equals(subCommand)
                && (CommandFeatureFlag.PORTAL.isVisibleTo(sender) || CommandFeatureFlag.PORTAL_CHECK.isVisibleTo(sender))) {
            return PortalCommandSupport.actionSuggestions(sender);
        }
        if ("world".equals(subCommand) && CommandFeatureFlag.WORLD.isVisibleTo(sender)) {
            return getWorldSuggestions(10);
        }
        if ("player".equals(subCommand) && CommandFeatureFlag.PLAYER.isVisibleTo(sender)) {
            return getOnlinePlayerNames();
        }
        if ("biome".equals(subCommand) && CommandFeatureFlag.BIOME.isVisibleTo(sender)) {
            if ("list".equalsIgnoreCase(args[1])) {
                return Arrays.asList("1", "2", "3");
            }
            return getBiomeArgumentSuggestions(args[1]);
        }
        if ("chunky".equals(subCommand) && CommandFeatureFlag.CHUNKY.isVisibleTo(sender)) {
            return Arrays.asList("stop");
        }
        if ("settings".equals(subCommand) && CommandFeatureFlag.SETTINGS.isVisibleTo(sender)) {
            return Arrays.asList("1", "2", "3", "4", "toggle");
        }
        if (!LocalFeatureGate.isLocalAdminBarsEnabled()) {
            return java.util.Collections.emptyList();
        }
        if ("allbars".equals(subCommand)
                && CommandFeatureFlag.ALL_BARS.isVisibleTo(sender)
                && Variables.getAdminBarService().shouldShowAllInTab(sender)) {
            return Arrays.asList("on", "off");
        }
        AdminBarType adminBarType = AdminBarType.fromSubCommand(subCommand);
        if (adminBarType != null
                && CommandFeatureFlag.fromSubCommand(subCommand).isVisibleTo(sender)
                && Variables.getAdminBarService().shouldShowInTab(sender, adminBarType)) {
            return Arrays.asList("on", "off");
        }
        return java.util.Collections.emptyList();
    }

    private List<String> getThirdArgumentSuggestions(CommandSender sender, String[] args) {
        String subCommand = args[0].toLowerCase();
        if ("portal".equals(subCommand)
                && CommandFeatureFlag.PORTAL.isVisibleTo(sender)
                && ("del".equalsIgnoreCase(args[1]) || "list".equalsIgnoreCase(args[1]))) {
            return PortalCommandSupport.portalNameSuggestions(sender, args[2], 8);
        }
        if ("player".equals(subCommand) && CommandFeatureFlag.PLAYER.isVisibleTo(sender)) {
            return getWorldSuggestions(10);
        }
        if ("chunky".equals(subCommand) && CommandFeatureFlag.CHUNKY.isVisibleTo(sender) && !"stop".equalsIgnoreCase(args[1])) {
            return Arrays.asList("stop");
        }
        if ("biome".equals(subCommand) && CommandFeatureFlag.BIOME.isVisibleTo(sender)) {
            if ("list".equalsIgnoreCase(args[1])) {
                return Arrays.asList("1", "2", "3");
            }
            return getSimpleBiomeSuggestions(args[2]);
        }
        if ("settings".equals(subCommand)
                && CommandFeatureFlag.SETTINGS.isVisibleTo(sender)
                && "toggle".equalsIgnoreCase(args[1])) {
            return getSettingsFlagSuggestions(args[2]);
        }
        return java.util.Collections.emptyList();
    }

    private void addAdminBarArguments(CommandSender sender, List<String> arguments) {
        if (!LocalFeatureGate.isLocalAdminBarsEnabled()) {
            return;
        }
        AdminBarService adminBarService = Variables.getAdminBarService();
        if (CommandFeatureFlag.TPS_BAR.isVisibleTo(sender) && adminBarService.shouldShowInTab(sender, AdminBarType.TPS)) {
            arguments.add("tpsbar");
        }
        if (CommandFeatureFlag.RAM_BAR.isVisibleTo(sender) && adminBarService.shouldShowInTab(sender, AdminBarType.RAM)) {
            arguments.add("rambar");
        }
        if (CommandFeatureFlag.MSPT_BAR.isVisibleTo(sender) && adminBarService.shouldShowInTab(sender, AdminBarType.MSPT)) {
            arguments.add("msptbar");
        }
        if (CommandFeatureFlag.ALL_BARS.isVisibleTo(sender) && adminBarService.shouldShowAllInTab(sender)) {
            arguments.add("allbars");
        }
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

    private List<String> getSettingsFlagSuggestions(String prefix) {
        String lowerPrefix = prefix == null ? "" : prefix.toLowerCase();
        List<String> suggestions = new ArrayList<String>();
        for (CommandFeatureFlag flag : CommandFeatureFlag.values()) {
            if (flag.isToggleable() && flag.getId().startsWith(lowerPrefix)) {
                suggestions.add(flag.getId());
            }
        }
        return suggestions;
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
        if ("list".startsWith(normalized)) {
            suggestions.add(prefix + "list");
        }
        for (String category : CommandRtpBiome.categoryNames()) {
            if (normalized.isEmpty() || category.startsWith(normalized)) {
                suggestions.add(prefix + category);
                if (enforceLimit && suggestions.size() >= 10) {
                    return suggestions;
                }
            }
        }
        for (String name : BIOME_NAMES) {
            if (normalized.isEmpty() || name.toLowerCase().startsWith(normalized)) {
                suggestions.add(prefix + name);
                if (enforceLimit && suggestions.size() >= 10) {
                    break;
                }
            }
        }
        return suggestions;
    }

    private static List<String> buildBiomeNames() {
        List<String> names = new ArrayList<>();
        for (Biome biome : Biome.values()) {
            names.add(biome.name());
        }
        return java.util.Collections.unmodifiableList(names);
    }
}
