package org.sRandomRTP.Services;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.ConfigValueParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AdminBarService {

    public static final String ALL_BARS_PERMISSION = "sRandomRTP.Command.AllBars";
    private static final double MAX_TPS = 20.0D;
    private static final String NOT_AVAILABLE = "N/A";
    /** Cached to avoid allocating a new array on every call to {@code AdminBarType.values()}. */
    private static final AdminBarType[] ALL_BAR_TYPES = AdminBarType.values();

    private final MessageService messageService;
    private final ServerMetricsProvider serverMetricsProvider;

    public AdminBarService(MessageService messageService) {
        this(messageService, new ServerMetricsProvider());
    }

    public AdminBarService(MessageService messageService, ServerMetricsProvider serverMetricsProvider) {
        this.messageService = messageService;
        this.serverMetricsProvider = serverMetricsProvider;
    }

    public boolean shouldShowInTab(CommandSender sender, AdminBarType type) {
        return sender instanceof Player
                && type != null
                && isEnabled(type)
                && isMetricAvailable(type)
                && sender.hasPermission(type.getPermissionNode());
    }

    public boolean shouldShowAllInTab(CommandSender sender) {
        if (!(sender instanceof Player)
                || !sender.hasPermission(ALL_BARS_PERMISSION)) {
            return false;
        }
        return !resolveEligibleTypes((Player) sender).isEmpty();
    }

    public boolean isEnabled(AdminBarType type) {
        FileConfiguration config = Variables.getPluginContext() != null ? Variables.getPluginContext().getConfigRegistry().getAdminBarsFile() : null;
        return type != null
                && config != null
                && config.getBoolean("admin-bars.enabled", true)
                && config.getBoolean(type.getConfigPath() + ".enabled", true);
    }

    public boolean isActive(Player player, AdminBarType type) {
        if (player == null || type == null) {
            return false;
        }
        RuntimeStateRegistry state = Variables.getRuntimeState();
        Set<AdminBarType> activeTypes = state.getAdminBarTypes().get(player);
        return activeTypes != null
                && activeTypes.contains(type)
                && state.getAdminBarTasks().containsKey(player);
    }

    public void sendPlayersOnly(CommandSender sender) {
        sendConfiguredMessage(sender, "admin-bars.messages.players-only");
    }

    public void sendUsage(CommandSender sender, AdminBarType type) {
        if (type == null) {
            return;
        }
        sendUsage(sender, type.getSubCommand());
    }

    public void sendDisabledInConfig(CommandSender sender) {
        sendConfiguredMessage(sender, "admin-bars.messages.command-disabled");
    }

    public void sendUsage(CommandSender sender, String command) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }
        sendConfiguredMessage(sender, "admin-bars.messages.usage", "%command%", command);
    }

    public void enable(Player player, AdminBarType type, boolean notify) {
        if (player == null || type == null) {
            return;
        }
        FileConfiguration config = getConfig();
        if (config == null) {
            return;
        }
        if (!isEnabled(type)) {
            sendDisabledInConfig(player);
            return;
        }
        if (!serverMetricsProvider.isMetricAvailable(type)) {
            sendConfiguredMessage(player, "admin-bars.messages.unavailable", "%bar%", type.getDisplayName());
            return;
        }

        boolean changed = activateType(player, type);
        if (notify && changed) {
            sendConfiguredMessage(player, "admin-bars.messages.enabled", "%bar%", type.getDisplayName());
        }
    }

    public void disable(Player player, AdminBarType type, boolean notify) {
        if (player == null || type == null) {
            return;
        }
        boolean changed = deactivateType(player, type);
        if (notify && changed) {
            sendConfiguredMessage(player, "admin-bars.messages.disabled", "%bar%", type.getDisplayName());
        }
    }

    public void enableAll(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        List<AdminBarType> enabledTypes = new ArrayList<>();
        for (AdminBarType type : resolveEligibleTypes(player)) {
            if (activateType(player, type)) {
                enabledTypes.add(type);
            }
        }
        if (notify) {
            if (enabledTypes.isEmpty()) {
                sendConfiguredMessage(player, "admin-bars.messages.unavailable", "%bar%", "Admin bars");
            } else {
                for (AdminBarType type : enabledTypes) {
                    sendConfiguredMessage(player, "admin-bars.messages.enabled", "%bar%", type.getDisplayName());
                }
            }
        }
    }

    public void disableAll(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        List<AdminBarType> activeTypes = new ArrayList<>(copyActiveTypes(player));
        for (AdminBarType type : activeTypes) {
            deactivateType(player, type);
            if (notify) {
                sendConfiguredMessage(player, "admin-bars.messages.disabled", "%bar%", type.getDisplayName());
            }
        }
    }

    public boolean areAllEligibleActive(Player player) {
        if (player == null) {
            return false;
        }
        Set<AdminBarType> eligibleTypes = resolveEligibleTypes(player);
        if (eligibleTypes.isEmpty()) {
            return false;
        }
        return copyActiveTypes(player).containsAll(eligibleTypes);
    }

    public void stop(Player player, boolean notify) {
        if (player == null) {
            return;
        }
        List<AdminBarType> activeTypes = new ArrayList<>(copyActiveTypes(player));
        cleanupPlayer(player);
        if (notify) {
            for (AdminBarType type : activeTypes) {
                sendConfiguredMessage(player, "admin-bars.messages.disabled", "%bar%", type.getDisplayName());
            }
        }
    }

    public void cleanupPlayer(Player player) {
        if (player == null) {
            return;
        }
        cancelTask(player);
        removeAllBars(player);
        Variables.getRuntimeState().getAdminBarTypes().remove(player);
    }

    public void shutdown() {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        for (WrappedTask task : state.getAdminBarTasks().values()) {
            if (task != null) {
                task.cancel();
            }
        }
        state.getAdminBarTasks().clear();

        for (Map<AdminBarType, BossBar> bossBars : state.getAdminBossBars().values()) {
            if (bossBars == null) {
                continue;
            }
            for (BossBar bossBar : bossBars.values()) {
                if (bossBar != null) {
                    bossBar.removeAll();
                }
            }
        }
        state.getAdminBossBars().clear();
        state.getAdminBarTypes().clear();
    }

    private void refreshBars(Player player) {
        if (player == null) {
            return;
        }
        if (!player.isOnline()) {
            cleanupPlayer(player);
            return;
        }
        Set<AdminBarType> activeTypes = copyActiveTypes(player);
        if (activeTypes.isEmpty()) {
            cleanupPlayer(player);
            return;
        }

        // Keep the RTP countdown bar visually clean by hiding the admin monitor bar
        // while a teleport countdown/search bar is active.
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state.hasTeleportTask(player) || state.getBossBars().containsKey(player)) {
            removeAllBars(player);
            return;
        }

        Map<AdminBarType, BossBar> bossBars = getOrCreateBossBarMap(player);
        for (AdminBarType type : ALL_BAR_TYPES) {
            if (!activeTypes.contains(type)) {
                removeBar(player, type);
                continue;
            }

            if (!isEnabled(type) || !player.hasPermission(type.getPermissionNode()) || !isMetricAvailable(type)) {
                deactivateType(player, type);
                continue;
            }

            MetricSnapshot snapshot = createSnapshot(type);
            if (snapshot == null) {
                removeBar(player, type);
                continue;
            }

            BossBar bossBar = bossBars.get(type);
            if (bossBar == null) {
                bossBar = Bukkit.createBossBar(snapshot.title, snapshot.color, snapshot.style);
                bossBar.setProgress(snapshot.progress);
                bossBar.addPlayer(player);
                bossBars.put(type, bossBar);
                continue;
            }

            bossBar.setTitle(snapshot.title);
            bossBar.setColor(snapshot.color);
            bossBar.setStyle(snapshot.style);
            bossBar.setProgress(snapshot.progress);
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        if (copyActiveTypes(player).isEmpty()) {
            return;
        }
        if (bossBars.isEmpty()) {
            state.getAdminBossBars().remove(player);
        }
    }

    private MetricSnapshot createSnapshot(AdminBarType type) {
        if (getConfig() == null) {
            return null;
        }
        switch (type) {
            case TPS:
                return createTpsSnapshot(type);
            case RAM:
                return createRamSnapshot(type);
            case MSPT:
                return createMsptSnapshot(type);
            default:
                return null;
        }
    }

    private MetricSnapshot createTpsSnapshot(AdminBarType type) {
        double tps = resolvePrimaryTps();
        String formattedTps = formatMetric(tps);
        double progress = Double.isNaN(tps) ? 0.0D : clamp(tps / MAX_TPS);
        BarColor color = resolveBarColor(type, tps, 18.0D, 15.0D, false);
        String title = formatTitle(type,
                "%value%", formattedTps,
                "%tps%", formattedTps);
        return new MetricSnapshot(title, progress, color, resolveStyle(type));
    }

    private MetricSnapshot createMsptSnapshot(AdminBarType type) {
        double mspt = resolveAverageTickTime();
        double maxScale = getConfig().getDouble(type.getConfigPath() + ".max-scale", 50.0D);
        String formattedMspt = formatMetric(mspt);
        double progress = Double.isNaN(mspt) ? 0.0D : clamp(mspt / Math.max(1.0D, maxScale));
        BarColor color = resolveBarColor(type, mspt, 25.0D, 40.0D, true);
        String title = formatTitle(type,
                "%value%", formattedMspt,
                "%mspt%", formattedMspt);
        return new MetricSnapshot(title, progress, color, resolveStyle(type));
    }

    private MetricSnapshot createRamSnapshot(AdminBarType type) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMb = bytesToMb(maxMemory);
        long usedMb = bytesToMb(usedMemory);
        long freeMb = Math.max(0L, maxMb - usedMb);
        double percent = maxMemory <= 0L ? 0.0D : (usedMemory * 100.0D) / maxMemory;
        double progress = clamp(percent / 100.0D);
        String formattedPercent = formatMetric(percent);
        BarColor color = resolveBarColor(type, percent, 60.0D, 80.0D, true);
        String title = formatTitle(type,
                "%value%", formattedPercent,
                "%used%", String.valueOf(usedMb),
                "%max%", String.valueOf(maxMb),
                "%free%", String.valueOf(freeMb),
                "%percent%", formattedPercent);
        return new MetricSnapshot(title, progress, color, resolveStyle(type));
    }

    /**
     * Resolves the boss-bar color based on thresholds.
     *
     * @param highIsBad {@code true} for metrics where a high value is bad (MSPT, RAM);
     *                  {@code false} for metrics where a low value is bad (TPS).
     */
    private BarColor resolveBarColor(AdminBarType type, double value,
                                     double firstThresholdDefault, double secondThresholdDefault,
                                     boolean highIsBad) {
        if (Double.isNaN(value)) {
            return BarColor.WHITE;
        }
        FileConfiguration cfg = getConfig();
        String base = type.getConfigPath();
        if (highIsBad) {
            double good    = cfg.getDouble(base + ".thresholds.good-at-or-below",    firstThresholdDefault);
            double warning = cfg.getDouble(base + ".thresholds.warning-at-or-below", secondThresholdDefault);
            if (value <= good)    return parseBarColor(cfg.getString(base + ".colors.good"),     BarColor.GREEN);
            if (value <= warning) return parseBarColor(cfg.getString(base + ".colors.warning"),  BarColor.YELLOW);
            return parseBarColor(cfg.getString(base + ".colors.critical"), BarColor.RED);
        } else {
            double warning  = cfg.getDouble(base + ".thresholds.warning-at-or-below",  firstThresholdDefault);
            double critical = cfg.getDouble(base + ".thresholds.critical-at-or-below", secondThresholdDefault);
            if (value <= critical) return parseBarColor(cfg.getString(base + ".colors.critical"), BarColor.RED);
            if (value <= warning)  return parseBarColor(cfg.getString(base + ".colors.warning"),  BarColor.YELLOW);
            return parseBarColor(cfg.getString(base + ".colors.good"), BarColor.GREEN);
        }
    }

    private BarStyle resolveStyle(AdminBarType type) {
        return parseBarStyle(getConfig().getString(type.getConfigPath() + ".style"), BarStyle.SEGMENTED_10);
    }

    private String formatTitle(AdminBarType type, String... replacements) {
        FileConfiguration config = getConfig();
        String title = config == null
                ? type.getDisplayName()
                : config.getString(type.getConfigPath() + ".title", type.getDisplayName());
        return messageService.format(title, replacements);
    }

    private void sendConfiguredMessage(CommandSender sender, String path, String... replacements) {
        if (sender == null) {
            return;
        }
        FileConfiguration config = getConfig();
        if (config == null) {
            return;
        }
        String line = config.getString(path);
        if (line == null || line.trim().isEmpty()) {
            return;
        }
        messageService.send(sender, Collections.singletonList(line), replacements);
    }

    private void cancelTask(Player player) {
        WrappedTask task = Variables.getRuntimeState().getAdminBarTasks().remove(player);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean activateType(Player player, AdminBarType type) {
        Set<AdminBarType> activeTypes = copyActiveTypes(player);
        boolean changed = activeTypes.add(type);
        Variables.getRuntimeState().getAdminBarTypes().put(player, activeTypes);
        ensureRefreshTask(player);
        FoliaSchedulerFacade.runAtEntity(player, () -> refreshBars(player));
        return changed;
    }

    private boolean deactivateType(Player player, AdminBarType type) {
        Set<AdminBarType> activeTypes = copyActiveTypes(player);
        boolean changed = activeTypes.remove(type);
        removeBar(player, type);
        if (activeTypes.isEmpty()) {
            cleanupPlayer(player);
        } else {
            Variables.getRuntimeState().getAdminBarTypes().put(player, activeTypes);
        }
        return changed;
    }

    private void ensureRefreshTask(Player player) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state.getAdminBarTasks().containsKey(player)) {
            return;
        }
        FileConfiguration config = getConfig();
        long interval = Math.max(1L, config == null ? 20L : config.getLong("admin-bars.update-interval-ticks", 20L));
        WrappedTask task = FoliaSchedulerFacade.runAtEntityTimer(player, () -> {
            if (player == null || !player.isOnline()) {
                cleanupPlayer(player);
                return;
            }
            refreshBars(player);
        }, 0L, interval);
        state.getAdminBarTasks().put(player, task);
    }

    /** Returns a snapshot of the active bar types for this player (never null). */
    private Set<AdminBarType> copyActiveTypes(Player player) {
        Set<AdminBarType> activeTypes = Variables.getRuntimeState().getAdminBarTypes().get(player);
        if (activeTypes == null || activeTypes.isEmpty()) {
            return EnumSet.noneOf(AdminBarType.class);
        }
        return EnumSet.copyOf(activeTypes);
    }

    private Set<AdminBarType> resolveEligibleTypes(Player player) {
        Set<AdminBarType> eligibleTypes = EnumSet.noneOf(AdminBarType.class);
        if (player == null) {
            return eligibleTypes;
        }
        for (AdminBarType type : ALL_BAR_TYPES) {
            if (isEnabled(type)
                    && player.hasPermission(type.getPermissionNode())
                    && isMetricAvailable(type)) {
                eligibleTypes.add(type);
            }
        }
        return eligibleTypes;
    }

    private Map<AdminBarType, BossBar> getOrCreateBossBarMap(Player player) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        Map<AdminBarType, BossBar> bossBars = state.getAdminBossBars().get(player);
        if (bossBars == null) {
            bossBars = new EnumMap<>(AdminBarType.class);
            state.getAdminBossBars().put(player, bossBars);
        }
        return bossBars;
    }

    private void removeBar(Player player, AdminBarType type) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        Map<AdminBarType, BossBar> bossBars = state.getAdminBossBars().get(player);
        if (bossBars == null || type == null) {
            return;
        }
        BossBar bossBar = bossBars.remove(type);
        if (bossBar != null) {
            bossBar.removeAll();
        }
        if (bossBars.isEmpty()) {
            state.getAdminBossBars().remove(player);
        } else {
            state.getAdminBossBars().put(player, bossBars);
        }
    }

    private void removeAllBars(Player player) {
        Map<AdminBarType, BossBar> bossBars = Variables.getRuntimeState().getAdminBossBars().remove(player);
        if (bossBars == null) {
            return;
        }
        for (BossBar bossBar : bossBars.values()) {
            if (bossBar != null) {
                bossBar.removeAll();
            }
        }
    }

    private FileConfiguration getConfig() {
        return Variables.getPluginContext() != null ? Variables.getPluginContext().getConfigRegistry().getAdminBarsFile() : null;
    }

    private boolean isMetricAvailable(AdminBarType type) {
        return serverMetricsProvider.isMetricAvailable(type);
    }

    private double resolvePrimaryTps() {
        return serverMetricsProvider.getPrimaryTps();
    }

    private double resolveAverageTickTime() {
        return serverMetricsProvider.getAverageTickTimeMs();
    }

    private String formatMetric(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return NOT_AVAILABLE;
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private long bytesToMb(long bytes) {
        return Math.max(0L, bytes / (1024L * 1024L));
    }

    private double clamp(double value) {
        if (Double.isNaN(value)) {
            return 0.0D;
        }
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }

    /** Parses an enum constant by name; returns {@code fallback} if blank or unknown. */
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        E parsed = ConfigValueParser.parseEnum(type, value);
        return parsed == null ? fallback : parsed;
    }

    private BarColor parseBarColor(String value, BarColor fallback) {
        return parseEnum(BarColor.class, value, fallback);
    }

    private BarStyle parseBarStyle(String value, BarStyle fallback) {
        return parseEnum(BarStyle.class, value, fallback);
    }

    private static final class MetricSnapshot {
        private final String title;
        private final double progress;
        private final BarColor color;
        private final BarStyle style;

        private MetricSnapshot(String title, double progress, BarColor color, BarStyle style) {
            this.title = title;
            this.progress = progress;
            this.color = color;
            this.style = style;
        }
    }
}
