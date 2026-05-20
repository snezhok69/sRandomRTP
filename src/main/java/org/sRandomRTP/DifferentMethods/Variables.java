package org.sRandomRTP.DifferentMethods;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.sRandomRTP.Utils.ChatUtils;

import org.bukkit.entity.Player;
import org.popcraft.chunky.api.ChunkyAPI;
import org.sRandomRTP.Commands.portal.PortalTeleportCooldownManager;
import org.sRandomRTP.Commands.portal.PortalTriggerHandler;
import org.sRandomRTP.Main;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.sRandomRTP.BlockBiomes.BiomeFilterSnapshot;
import org.sRandomRTP.Services.AdminBarService;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.Services.ConfigVersionSupport;
import org.sRandomRTP.Services.MessageService;
import org.sRandomRTP.Services.PluginContext;
import org.sRandomRTP.Services.PortalRepository;
import org.sRandomRTP.Services.ReleaseCheckService;
import org.sRandomRTP.Services.RngProvider;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Services.ServerMetricsProvider;
import org.sRandomRTP.Services.TeleportMetrics;
import org.sRandomRTP.Services.TeleportService;
    
public class Variables {
    private static volatile RuntimeStateRegistry runtimeState;
    //
    // MIGRATING: use PluginContext directly instead of the static field
    public static volatile Main instance;
    public static ChunkyAPI chunkyAPI;

    public static Main getInstance() {
        return instance;
    }

    /**
     * Returns {@code true} when diagnostics are enabled.
     * Reads from the atomic {@link #configCache} snapshot — no live YAML access.
     * Falls back to the live config only before the first config load (startup).
     */
    public static boolean isDiagnosticEnabled() {
        if (configCache != null && configCache != org.sRandomRTP.Services.ConfigCache.DEFAULT) {
            return configCache.loggingEnabled;
        }
        // Pre-load fallback — only reached during plugin startup
        if (instance == null) return false;
        org.bukkit.configuration.file.FileConfiguration cfg = instance.getConfig();
        return org.sRandomRTP.Services.ConfigCache.readDiagnosticEnabled(cfg);
    }

    public static boolean isLoggingEnabled() {
        return isDiagnosticEnabled();
    }

    //
    public static volatile Map<Material, Integer> itemMap = Collections.emptyMap();
    //
    // ── Config cache — single atomic snapshot of all parsed config scalars ───
    /**
     * Immutable snapshot of all cached config values.
     * Replaced atomically on every /reload via {@code configCache = ConfigCache.buildFrom(registry)}.
     * Eliminates 55 separate non-atomic volatile writes during config reload.
     */
    public static volatile ConfigCache configCache = ConfigCache.DEFAULT;

    public static volatile Set<Material> blockList = Collections.emptySet();

    // ── Biome filter — single atomic snapshot ────────────────────────────────
    /** Atomically-replaceable biome filter snapshot. Replaced on every config reload. */
    public static volatile BiomeFilterSnapshot biomeFilters = BiomeFilterSnapshot.EMPTY;

    // Version cached once at startup — not part of ConfigCache (read from Bukkit, not config files)
    public static int cachedServerMajorVersion = 0;
    //
    // MIGRATING: task handles; move ownership to PluginContext.cancelBackgroundTasks()
    public static volatile WrappedTask autoCheckVersionTask;
    public static volatile WrappedTask commandReloadTask;
    //
    //
    public static void clearTeleportFlags(Player player) {
        RuntimeStateRegistry rs = runtimeState;
        if (rs != null) rs.clearTeleportFlags(player);
    }

    // Removes expired cooldown entries to prevent unbounded memory growth.
    // Safe to call from any thread since maps are ConcurrentHashMap.
    public static void cleanExpiredCooldowns(long maxCooldownMs) {
        RuntimeStateRegistry rs = runtimeState;
        if (rs != null) rs.cleanExpiredCooldowns(maxCooldownMs);
    }

    //
    private static volatile PluginContext pluginContext;

    public static FoliaLib getFoliaLib() {
        return pluginContext != null ? pluginContext.getFoliaLib() : null;
    }

    /**
     * Returns the runtime state registry.
     * Returns {@code null} before {@link #initializePlugin(Main)} completes —
     * callers that may run during early startup must null-check the result.
     */
    public static RuntimeStateRegistry getRuntimeState() {
        return runtimeState;
    }

    public static PluginContext getPluginContext() {
        return pluginContext;
    }

    /**
     * Returns a service from PluginContext.
     * Throws IllegalStateException if PluginContext is not yet initialized —
     * callers must not invoke service getters before initializePlugin() completes.
     */
    private static <T> T ctx(Function<PluginContext, T> fn) {
        if (pluginContext == null) {
            throw new IllegalStateException(
                "[sRandomRTP] PluginContext is not initialized. Did you call Variables.initializePlugin() first?");
        }
        return fn.apply(pluginContext);
    }

    // Services with zero-dependency defaults — safe to call before full initialization
    public static MessageService getMessageService() {
        return pluginContext != null ? pluginContext.getMessageService() : new MessageService();
    }

    public static RngProvider getRngProvider() {
        return pluginContext != null ? pluginContext.getRngProvider() : new RngProvider();
    }

    public static ServerMetricsProvider getServerMetricsProvider() {
        return pluginContext != null ? pluginContext.getServerMetricsProvider() : new ServerMetricsProvider();
    }

    // Services that may be null before plugin is fully started — return null gracefully
    public static PortalRepository getPortalRepository() {
        return pluginContext != null ? pluginContext.getPortalRepository() : null;
    }

    public static TeleportMetrics getTeleportMetrics() {
        return pluginContext != null ? pluginContext.getTeleportMetrics() : null;
    }

    public static ConfigVersionSupport getConfigVersionSupport() {
        return pluginContext != null ? pluginContext.getConfigVersionSupport() : null;
    }

    // Services that require PluginContext — throw on null to prevent silent failures
    public static PortalTeleportCooldownManager getPortalTeleportCooldownManager() {
        return pluginContext != null ? pluginContext.getPortalTeleportCooldownManager() : null;
    }

    public static PortalTriggerHandler getPortalTriggerHandler() {
        return pluginContext != null ? pluginContext.getPortalTriggerHandler() : null;
    }

    public static TeleportService getTeleportService() {
        return ctx(PluginContext::getTeleportService);
    }

    public static AdminBarService getAdminBarService() {
        return ctx(PluginContext::getAdminBarService);
    }

    public static ReleaseCheckService getReleaseCheckService() {
        return ctx(PluginContext::getReleaseCheckService);
    }

    // MIGRATING: pluginContext — prefer injecting PluginContext through constructors
    //
    public static void initializePlugin(Main plugin) {
        instance = plugin;
        if (pluginContext == null || pluginContext.getPlugin() != plugin) {
            pluginContext = new PluginContext(plugin);
        }
        // Always use the RuntimeStateRegistry from PluginContext — never create an orphan fallback.
        // If PluginContext.getRuntimeStateRegistry() returns null it is a programming error: fix PluginContext.
        RuntimeStateRegistry contextRegistry = pluginContext.getRuntimeStateRegistry();
        if (contextRegistry == null) {
            throw new IllegalStateException("[sRandomRTP] PluginContext returned null RuntimeStateRegistry — check PluginContext initialization.");
        }
        runtimeState = contextRegistry;
        // Cache server major version once at plugin startup
        // Bukkit.getBukkitVersion() → "1.19.4-R0.1-SNAPSHOT" — split by [.\-] gives [1]="19" etc.
        try {
            String ver = Bukkit.getBukkitVersion(); // "1.19.4-R0.1-SNAPSHOT"
            String[] parts = ver.split("[.\\-]");   // ["1","19","4","R0","1","SNAPSHOT"]
            cachedServerMajorVersion = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            cachedServerMajorVersion = 17; // safe fallback — supports freeze
        }
    }
}
