package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.sRandomRTP.Services.ConfigDefaults;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ConfiguredCommandAliases {

    public static final String CONFIG_PATH = "Command-Aliases";
    public static final String ENABLED_CONFIG_PATH = "Command-Aliases-Enabled";
    private static final String MAIN_COMMAND = "rtp";
    private static final Map<String, AliasCommand> REGISTERED_ALIASES = new LinkedHashMap<String, AliasCommand>();

    private ConfiguredCommandAliases() {
    }

    public static synchronized void apply(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }

        PluginCommand rtpCommand = plugin.getCommand(MAIN_COMMAND);
        if (rtpCommand == null) {
            plugin.getLogger().warning("Cannot apply configured command aliases: /rtp is not registered.");
            return;
        }

        List<String> aliases = getConfiguredAliases(plugin.getConfig());
        unregister(plugin, rtpCommand, false);
        rtpCommand.setAliases(aliases);

        if (aliases.isEmpty()) {
            syncCommands(plugin);
            return;
        }

        CommandMap commandMap = getCommandMap(plugin);
        if (commandMap == null) {
            plugin.getLogger().warning("Cannot apply configured command aliases: Bukkit command map is unavailable.");
            syncCommands(plugin);
            return;
        }

        Map<String, Command> knownCommands = getKnownCommands(commandMap, plugin);
        String fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
        for (String alias : aliases) {
            Command existing = knownCommands == null ? commandMap.getCommand(alias) : knownCommands.get(alias);
            if (existing != null && !isOurAlias(existing, plugin)) {
                plugin.getLogger().warning("Command alias '/" + alias + "' is already used by another command and was skipped.");
                continue;
            }

            AliasCommand aliasCommand = new AliasCommand(plugin, rtpCommand, alias);
            if (commandMap.register(fallbackPrefix, aliasCommand)) {
                REGISTERED_ALIASES.put(alias, aliasCommand);
            } else {
                plugin.getLogger().warning("Command alias '/" + alias + "' could not be registered.");
            }
        }
        syncCommands(plugin);
    }

    public static synchronized void unregister(JavaPlugin plugin) {
        PluginCommand rtpCommand = plugin == null ? null : plugin.getCommand(MAIN_COMMAND);
        unregister(plugin, rtpCommand, true);
    }

    static List<String> sanitizeAliases(List<String> configuredAliases) {
        if (configuredAliases == null || configuredAliases.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> aliases = new LinkedHashSet<String>();
        for (String configuredAlias : configuredAliases) {
            if (configuredAlias == null) {
                continue;
            }
            String alias = configuredAlias.trim().toLowerCase(Locale.ROOT);
            while (alias.startsWith("/")) {
                alias = alias.substring(1).trim();
            }
            if (alias.isEmpty() || MAIN_COMMAND.equals(alias) || !alias.matches("[a-z0-9_-]+")) {
                continue;
            }
            aliases.add(alias);
        }
        return Collections.unmodifiableList(new ArrayList<String>(aliases));
    }

    static List<String> getConfiguredAliases(FileConfiguration config) {
        if (config != null && !config.getBoolean(ENABLED_CONFIG_PATH, ConfigDefaults.COMMAND_ALIASES_ENABLED)) {
            return Collections.emptyList();
        }
        if (config == null || !config.contains(CONFIG_PATH)) {
            return ConfigDefaults.COMMAND_ALIASES;
        }
        return sanitizeAliases(config.getStringList(CONFIG_PATH));
    }

    private static void unregister(JavaPlugin plugin, PluginCommand rtpCommand, boolean sync) {
        if (plugin == null) {
            return;
        }

        CommandMap commandMap = getCommandMap(plugin);
        if (commandMap != null) {
            Map<String, Command> knownCommands = getKnownCommands(commandMap, plugin);
            String fallbackPrefix = plugin.getName().toLowerCase(Locale.ROOT);
            for (Map.Entry<String, AliasCommand> entry : new ArrayList<Map.Entry<String, AliasCommand>>(REGISTERED_ALIASES.entrySet())) {
                String alias = entry.getKey();
                AliasCommand command = entry.getValue();
                if (knownCommands != null) {
                    removeIfSame(knownCommands, alias, command);
                    removeIfSame(knownCommands, fallbackPrefix + ":" + alias, command);
                }
                command.unregister(commandMap);
            }
        }
        REGISTERED_ALIASES.clear();

        if (rtpCommand != null) {
            rtpCommand.setAliases(Collections.<String>emptyList());
        }
        if (sync) {
            syncCommands(plugin);
        }
    }

    private static void removeIfSame(Map<String, Command> knownCommands, String key, Command command) {
        Command current = knownCommands.get(key);
        if (current == command) {
            knownCommands.remove(key);
        }
    }

    private static boolean isOurAlias(Command command, Plugin plugin) {
        return command instanceof AliasCommand && ((AliasCommand) command).getPlugin() == plugin;
    }

    private static CommandMap getCommandMap(JavaPlugin plugin) {
        try {
            Field field = findField(Bukkit.getServer().getClass(), "commandMap");
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(Bukkit.getServer());
            return value instanceof CommandMap ? (CommandMap) value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("Failed to access Bukkit command map: " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> getKnownCommands(CommandMap commandMap, JavaPlugin plugin) {
        try {
            Field field = findField(commandMap.getClass(), "knownCommands");
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(commandMap);
            return value instanceof Map ? (Map<String, Command>) value : null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("Failed to access Bukkit known commands: " + e.getMessage());
            return null;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static void syncCommands(JavaPlugin plugin) {
        try {
            Method method = Bukkit.getServer().getClass().getMethod("syncCommands");
            method.invoke(Bukkit.getServer());
        } catch (NoSuchMethodException ignored) {
            // Older 1.16 cores can work without a Brigadier command tree sync.
        } catch (ReflectiveOperationException | RuntimeException e) {
            plugin.getLogger().warning("Failed to sync command aliases with the server command tree: " + e.getMessage());
        }
    }

    private static final class AliasCommand extends Command implements PluginIdentifiableCommand {
        private final JavaPlugin plugin;
        private final PluginCommand target;

        private AliasCommand(JavaPlugin plugin, PluginCommand target, String alias) {
            super(alias, target.getDescription(), target.getUsage(), Collections.<String>emptyList());
            this.plugin = plugin;
            this.target = target;
            setPermission(target.getPermission());
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return target.execute(sender, MAIN_COMMAND, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
            return target.tabComplete(sender, MAIN_COMMAND, args);
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
