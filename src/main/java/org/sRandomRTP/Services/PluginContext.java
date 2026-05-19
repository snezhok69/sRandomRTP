package org.sRandomRTP.Services;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.sRandomRTP.Commands.portal.PortalTeleportCooldownManager;
import org.sRandomRTP.Commands.portal.PortalTriggerHandler;
import org.sRandomRTP.Main;

/**
 * Dependency-injection container for all plugin singletons.
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Constructed during {@code Variables.initializePlugin()} — all non-optional services
 *       are fully initialised at this point.</li>
 *   <li>Optional integration services ({@link #getEconomy()}) are populated later during
 *       {@link #setupEconomy()} which runs after Bukkit's plugin-manager scan.</li>
 *   <li>Destroyed on plugin disable via {@code Main.onDisable()}.</li>
 * </ol>
 *
 * <h3>Null contract</h3>
 * <ul>
 *   <li>All getters except {@link #getEconomy()} are guaranteed non-null after construction.</li>
 *   <li>{@link #getEconomy()} returns {@code null} when Vault is not installed or the economy
 *       provider was not registered — always null-check before use.</li>
 * </ul>
 */
public class PluginContext {

    private final Main plugin;
    private final FoliaLib foliaLib;
    private final ConfigRegistry configRegistry;

    // ── Integration flags (set after plugin-manager scan) ───────────────────
    private volatile boolean vaultAvailable;
    private volatile boolean worldGuardAvailable;
    private volatile boolean pluginToggle;
    private volatile Economy economy;

    // ── Background task lifecycle ────────────────────────────────────────────
    /** Periodic cleanup task for expired cooldowns. Owned here for clean onDisable cancellation. */
    private volatile WrappedTask cooldownCleanupTask;
    private final MessageService messageService;
    private final RngProvider rngProvider;
    private final TeleportMetrics teleportMetrics;
    private final PortalRepository portalRepository;
    private final MigrationRunner migrationRunner;
    private final BootstrapCoordinator bootstrapCoordinator;
    private final TeleportService teleportService;
    private final DiagnosticsService diagnosticsService;
    private final AdminBarService adminBarService;
    private final ConfigVersionSupport configVersionSupport;
    private final ServerMetricsProvider serverMetricsProvider;
    private final ReleaseCheckService releaseCheckService;
    private final RuntimeStateRegistry runtimeStateRegistry;
    private final PortalTeleportCooldownManager portalTeleportCooldownManager;
    private final PortalTriggerHandler portalTriggerHandler;

    public PluginContext(Main plugin) {
        this.plugin = plugin;
        this.foliaLib = new FoliaLib(plugin);
        this.runtimeStateRegistry = new RuntimeStateRegistry();
        this.portalTeleportCooldownManager = new PortalTeleportCooldownManager();
        this.portalTriggerHandler = new PortalTriggerHandler(portalTeleportCooldownManager);
        this.configRegistry = new ConfigRegistry(plugin);
        this.messageService = new MessageService();
        this.rngProvider = new RngProvider();
        this.teleportMetrics = new TeleportMetrics(plugin);
        this.portalRepository = new PortalRepository(plugin);
        this.migrationRunner = new MigrationRunner(plugin, configRegistry, portalRepository);
        this.teleportService = new TeleportService(messageService);
        this.diagnosticsService = new DiagnosticsService(plugin);
        this.configVersionSupport = new ConfigVersionSupport(configRegistry, plugin.getLogger());
        this.serverMetricsProvider = new ServerMetricsProvider();
        this.releaseCheckService = new ReleaseCheckService(messageService);
        this.adminBarService = new AdminBarService(messageService, serverMetricsProvider);
        this.bootstrapCoordinator = new BootstrapCoordinator(plugin, this);
    }

    public Main getPlugin() {
        return plugin;
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    // ── Integration flags ────────────────────────────────────────────────────

    public boolean isVaultAvailable()      { return vaultAvailable; }
    public boolean isWorldGuardAvailable() { return worldGuardAvailable; }
    public boolean isPluginToggle()        { return pluginToggle; }
    /** @return the Vault economy provider, or {@code null} if Vault is not installed. */
    public Economy getEconomy()            { return economy; }

    public void setVaultAvailable(boolean v)      { this.vaultAvailable = v; }
    public void setWorldGuardAvailable(boolean v) { this.worldGuardAvailable = v; }
    public void setPluginToggle(boolean v)        { this.pluginToggle = v; }

    /** Attempts to set up Vault economy. Silently skips if Vault is not installed. */
    public void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) return;
        RegisteredServiceProvider<Economy> rsp =
                Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            this.economy = rsp.getProvider();
        }
    }

    public ConfigRegistry getConfigRegistry() {
        return configRegistry;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public RngProvider getRngProvider() {
        return rngProvider;
    }

    public TeleportMetrics getTeleportMetrics() {
        return teleportMetrics;
    }

    public PortalRepository getPortalRepository() {
        return portalRepository;
    }

    public MigrationRunner getMigrationRunner() {
        return migrationRunner;
    }

    public BootstrapCoordinator getBootstrapCoordinator() {
        return bootstrapCoordinator;
    }

    public TeleportService getTeleportService() {
        return teleportService;
    }

    public DiagnosticsService getDiagnosticsService() {
        return diagnosticsService;
    }

    public AdminBarService getAdminBarService() {
        return adminBarService;
    }

    public ConfigVersionSupport getConfigVersionSupport() {
        return configVersionSupport;
    }

    public ServerMetricsProvider getServerMetricsProvider() {
        return serverMetricsProvider;
    }

    public ReleaseCheckService getReleaseCheckService() {
        return releaseCheckService;
    }

    public RuntimeStateRegistry getRuntimeStateRegistry() {
        return runtimeStateRegistry;
    }

    public PortalTeleportCooldownManager getPortalTeleportCooldownManager() {
        return portalTeleportCooldownManager;
    }

    public PortalTriggerHandler getPortalTriggerHandler() {
        return portalTriggerHandler;
    }

    public WrappedTask getCooldownCleanupTask()                   { return cooldownCleanupTask; }
    public void setCooldownCleanupTask(WrappedTask task)          { this.cooldownCleanupTask = task; }

    /** Cancels all background tasks managed by PluginContext. Called from Main.onDisable(). */
    public void cancelBackgroundTasks() {
        if (cooldownCleanupTask != null) {
            cooldownCleanupTask.cancel();
            cooldownCleanupTask = null;
        }
    }
}
