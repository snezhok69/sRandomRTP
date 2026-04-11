package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.Main;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

public class MigrationRunner {

    private final Logger logger;
    private final ConfigRegistry configRegistry;
    private final PortalRepository portalRepository;

    public MigrationRunner(Main plugin, ConfigRegistry configRegistry, PortalRepository portalRepository) {
        this(plugin.getLogger(), configRegistry, portalRepository);
    }

    public MigrationRunner(Logger logger, ConfigRegistry configRegistry, PortalRepository portalRepository) {
        this.logger = logger;
        this.configRegistry = configRegistry;
        this.portalRepository = portalRepository;
    }

    public void runConfigMigrations() {
        migrateLegacySlowRequestThreshold();
        for (File file : configRegistry.getManagedConfigFiles()) {
            if (!file.exists()) {
                continue;
            }
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int storedVersion = yaml.getInt(PluginVersionCatalog.CONFIG_VERSION_PATH, 0);
            if (storedVersion >= PluginVersionCatalog.CONFIG_VERSION) {
                continue;
            }
            applyManagedDefaults(file, yaml);
            yaml.set(PluginVersionCatalog.CONFIG_VERSION_PATH, PluginVersionCatalog.CONFIG_VERSION);
            try {
                yaml.save(file);
            } catch (IOException e) {
                logger.warning("Failed to update config version for " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void runDatabaseMigrations() {
        try {
            portalRepository.openAsync().get();
            Connection connection = portalRepository.getConnection();
            portalRepository.ensureSchema(connection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Database migration interrupted: " + e.getMessage());
        } catch (ExecutionException | SQLException e) {
            logger.warning("Failed to run database migrations: " + e.getMessage());
        }
    }

    private void applyManagedDefaults(File file, YamlConfiguration yaml) {
        if (file == null || yaml == null) {
            return;
        }

        String fileName = file.getName();
        if ("config.yml".equalsIgnoreCase(fileName)) {
            setIfMissing(yaml, "metrics.rtp.slow-request-threshold-ms", 3000L);
        } else if ("teleport.yml".equalsIgnoreCase(fileName)) {
            setIfMissing(yaml, "teleport.prefer-generated-chunks.enabled", false);
            setIfMissing(yaml, "teleport.prefer-generated-chunks.window-ms", 1000L);
            setIfMissing(yaml, "teleport.prefer-generated-chunks.max-attempts", 8);
            setIfMissing(yaml, "teleport.parallel-search.enabled", false);
            setIfMissing(yaml, "teleport.parallel-search.candidates-per-batch", 2);
            setIfMissing(yaml, "teleport.parallel-search.max-global-inflight", 24);
        } else if ("admin-bars.yml".equalsIgnoreCase(fileName)) {
            setIfMissing(yaml, "admin-bars.enabled", true);
            setIfMissing(yaml, "admin-bars.update-interval-ticks", 20L);
            setIfMissing(yaml, "admin-bars.messages.enabled", "&a[sRandomRTP] &a%bar% enabled.");
            setIfMissing(yaml, "admin-bars.messages.disabled", "&a[sRandomRTP] &e%bar% disabled.");
            setIfMissing(yaml, "admin-bars.messages.unavailable", "&a[sRandomRTP] &cThis metric is not available on this server core.");
            setIfMissing(yaml, "admin-bars.messages.command-disabled", "&a[sRandomRTP] &cThis command is disabled in Settings/admin-bars.yml.");
            setIfMissing(yaml, "admin-bars.messages.players-only", "&a[sRandomRTP] &cOnly players can use this command.");
            setIfMissing(yaml, "admin-bars.messages.usage", "&a[sRandomRTP] &6Usage: /rtp %command% [on|off]");
            setIfMissing(yaml, "admin-bars.tpsbar.enabled", true);
            setIfMissing(yaml, "admin-bars.rambar.enabled", true);
            setIfMissing(yaml, "admin-bars.msptbar.enabled", true);
        }
    }

    private void setIfMissing(YamlConfiguration yaml, String path, Object value) {
        if (!yaml.contains(path)) {
            yaml.set(path, value);
        }
    }

    private void migrateLegacySlowRequestThreshold() {
        File configFile = configRegistry.resolve("config.yml");
        File teleportFile = configRegistry.resolve("Settings/teleport.yml");
        if (!configFile.exists() || !teleportFile.exists()) {
            return;
        }

        YamlConfiguration configYaml = YamlConfiguration.loadConfiguration(configFile);
        if (configYaml.contains("metrics.rtp.slow-request-threshold-ms")) {
            return;
        }

        YamlConfiguration teleportYaml = YamlConfiguration.loadConfiguration(teleportFile);
        if (!teleportYaml.contains("teleport.metrics.slow-request-threshold-ms")) {
            return;
        }

        configYaml.set("metrics.rtp.slow-request-threshold-ms",
                teleportYaml.getLong("teleport.metrics.slow-request-threshold-ms", 3000L));
        try {
            configYaml.save(configFile);
        } catch (IOException e) {
            logger.warning("Failed to migrate slow RTP threshold into config.yml: " + e.getMessage());
        }
    }
}
