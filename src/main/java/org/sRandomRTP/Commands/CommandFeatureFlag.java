package org.sRandomRTP.Commands;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Services.LocalFeatureGate;
import org.sRandomRTP.Services.PluginVersionCatalog;
import org.sRandomRTP.Utils.ChatUtils;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum CommandFeatureFlag {
    RTP("rtp", "Player", "commands.player.rtp.enabled", "/rtp", Permissions.RTP, true, false, true),
    NEAR("near", "Player", "commands.player.near.enabled", "/rtp near", Permissions.NEAR, true, false, true),
    FAR("far", "Player", "commands.player.far.enabled", "/rtp far", Permissions.FAR, true, false, true),
    MIDDLE("middle", "Player", "commands.player.middle.enabled", "/rtp middle", Permissions.MIDDLE, true, false, true),
    BASE("base", "Player", "commands.player.base.enabled", "/rtp base", Permissions.BASE, true, false, true),
    BIOME("biome", "Player", "commands.player.biome.enabled", "/rtp biome", Permissions.RTP_BIOME, true, false, true),
    WORLD("world", "Player", "commands.player.world.enabled", "/rtp world", Permissions.WORLD, true, false, true),
    BACK("back", "Player", "commands.player.back.enabled", "/rtp back", Permissions.BACK, true, false, true),
    CANCEL("cancel", "Player", "commands.player.cancel.enabled", "/rtp cancel", Permissions.CANCEL, true, false, true),
    ACCEPT("accept", "Player", "commands.player.accept.enabled", "/rtp accept", Permissions.ACCEPT, true, false, true),
    DENY("deny", "Player", "commands.player.deny.enabled", "/rtp deny", Permissions.DENY, true, false, true),

    PLAYER("player", "Admin", "commands.admin.player.enabled", "/rtp player", Permissions.PLAYER, true, false, true),
    PORTAL("portal", "Admin", "commands.admin.portal.enabled", "/rtp portal set/del/list", Permissions.PORTAL, true, false, true),
    CHUNKY("chunky", "Admin", "commands.admin.chunky.enabled", "/rtp chunky", Permissions.CHUNKY, true, false, true),
    VERSION("version", "Admin", "commands.admin.version.enabled", "/rtp version", Permissions.VERSION, true, false, true),
    RELOAD("reload", "Admin", "commands.admin.reload.enabled", "/rtp reload", Permissions.RELOAD, true, false, true),
    HELP("help", "Admin", "commands.admin.help.enabled", "/rtp help", Permissions.HELP, true, false, true),
    SETTINGS("settings", "Admin", "commands.admin.settings.enabled", "/rtp settings", Permissions.SETTINGS, true, false, false),

    DOCTOR("doctor", "Debug", "commands.debug.doctor.enabled", "/rtp doctor", Permissions.DOCTOR, true, false, true),
    DUMP("dump", "Debug", "commands.debug.dump.enabled", "/rtp dump", Permissions.DUMP, true, false, true),
    STATS("stats", "Debug", "commands.debug.stats.enabled", "/rtp stats", Permissions.STATS, true, false, true),
    PORTAL_CHECK("portal-check", "Debug", "commands.debug.portal-check.enabled", "/rtp portal check", Permissions.DOCTOR, true, false, true),
    ALL_BARS("allbars", "Debug", "commands.debug.allbars.enabled", "/rtp allbars", Permissions.ALL_BARS, true, true, true),
    TPS_BAR("tpsbar", "Debug", "commands.debug.tpsbar.enabled", "/rtp tpsbar", Permissions.TPS_BAR, true, true, true),
    RAM_BAR("rambar", "Debug", "commands.debug.rambar.enabled", "/rtp rambar", Permissions.RAM_BAR, true, true, true),
    MSPT_BAR("msptbar", "Debug", "commands.debug.msptbar.enabled", "/rtp msptbar", Permissions.MSPT_BAR, true, true, true);

    private static final Map<String, CommandFeatureFlag> BY_ID;
    private static final Map<String, CommandFeatureFlag> BY_SUB_COMMAND;

    static {
        Map<String, CommandFeatureFlag> byId = new LinkedHashMap<>();
        Map<String, CommandFeatureFlag> bySubCommand = new LinkedHashMap<>();
        for (CommandFeatureFlag flag : values()) {
            byId.put(flag.id, flag);
            if (!flag.id.contains("-")) {
                bySubCommand.put(flag.id, flag);
            }
        }
        BY_ID = Collections.unmodifiableMap(byId);
        BY_SUB_COMMAND = Collections.unmodifiableMap(bySubCommand);
    }

    private final String id;
    private final String category;
    private final String configPath;
    private final String commandLabel;
    private final String permission;
    private final boolean defaultEnabled;
    private final boolean localOnly;
    private final boolean toggleable;

    CommandFeatureFlag(String id, String category, String configPath, String commandLabel, String permission,
                       boolean defaultEnabled, boolean localOnly, boolean toggleable) {
        this.id = id;
        this.category = category;
        this.configPath = configPath;
        this.commandLabel = commandLabel;
        this.permission = permission;
        this.defaultEnabled = defaultEnabled;
        this.localOnly = localOnly;
        this.toggleable = toggleable;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getConfigPath() {
        return configPath;
    }

    public String getCommandLabel() {
        return commandLabel;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public boolean isToggleable() {
        return toggleable;
    }

    public boolean isLocalGateOpen() {
        return !localOnly || LocalFeatureGate.isLocalAdminBarsEnabled();
    }

    public boolean isConfiguredEnabled() {
        FileConfiguration commands = getCommandsConfig();
        return commands == null ? defaultEnabled : commands.getBoolean(configPath, defaultEnabled);
    }

    public boolean isEnabled() {
        return isConfiguredEnabled() && isLocalGateOpen();
    }

    public boolean isVisibleTo(CommandSender sender) {
        return sender != null && sender.hasPermission(permission) && isEnabled();
    }

    public void sendDisabled(CommandSender sender) {
        sender.sendMessage(ChatUtils.PLUGIN_NAME + " §cThis command is disabled in §fSettings/commands.yml§c: §e" + id);
    }

    public static CommandFeatureFlag fromId(String id) {
        if (id == null) {
            return null;
        }
        return BY_ID.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public static CommandFeatureFlag fromSubCommand(String subCommand) {
        if (subCommand == null) {
            return null;
        }
        return BY_SUB_COMMAND.get(subCommand.toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean ensureEnabled(CommandSender sender, CommandFeatureFlag flag) {
        if (flag == null || flag.isEnabled()) {
            return true;
        }
        flag.sendDisabled(sender);
        return false;
    }

    public static boolean ensurePermissionAndEnabled(CommandSender sender, CommandFeatureFlag flag) {
        if (flag == null) {
            return true;
        }
        if (!CommandUtils.checkPermission(sender, flag.getPermission())) {
            return false;
        }
        return ensureEnabled(sender, flag);
    }

    public static void setEnabled(CommandFeatureFlag flag, boolean enabled) throws IOException {
        if (flag == null) {
            return;
        }
        File file = getCommandsFile();
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.contains(PluginVersionCatalog.CONFIG_VERSION_PATH)) {
            yaml.set(PluginVersionCatalog.CONFIG_VERSION_PATH, PluginVersionCatalog.CONFIG_VERSION);
        }
        yaml.set(flag.getConfigPath(), enabled);
        yaml.save(file);
        reloadCommandsConfig();
    }

    public static void reloadCommandsConfig() {
        if (Variables.getPluginContext() != null) {
            ConfigRegistry registry = Variables.getPluginContext().getConfigRegistry();
            registry.reload();
            Variables.configCache = ConfigCache.buildFrom(registry,
                    Variables.getInstance() != null ? Variables.getInstance().getConfig() : null);
        }
    }

    public static FileConfiguration getCommandsConfig() {
        if (Variables.getPluginContext() != null) {
            FileConfiguration commands = Variables.getPluginContext().getConfigRegistry().getCommandsFile();
            if (commands != null) {
                return commands;
            }
        }
        File file = getCommandsFile();
        return file.exists() ? YamlConfiguration.loadConfiguration(file) : null;
    }

    private static File getCommandsFile() {
        return new File(Variables.getInstance().getDataFolder(), "Settings/commands.yml");
    }
}
