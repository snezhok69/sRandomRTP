package org.sRandomRTP.Services;

import org.sRandomRTP.Main;

public class PluginContext {

    private final Main plugin;
    private final ConfigRegistry configRegistry;
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

    public PluginContext(Main plugin) {
        this.plugin = plugin;
        this.runtimeStateRegistry = new RuntimeStateRegistry();
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
}
