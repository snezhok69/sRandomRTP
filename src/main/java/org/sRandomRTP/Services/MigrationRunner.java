package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.Main;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
            boolean versionNeedsBump = storedVersion < PluginVersionCatalog.CONFIG_VERSION;
            boolean modified = applyManagedDefaults(file, yaml);
            if (!versionNeedsBump && !modified) {
                continue;
            }
            if (versionNeedsBump) {
                yaml.set(PluginVersionCatalog.CONFIG_VERSION_PATH, PluginVersionCatalog.CONFIG_VERSION);
            }
            try {
                yaml.save(file);
            } catch (IOException e) {
                logger.warning("Failed to update config version for " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    public void runDatabaseMigrations() {
        try {
            portalRepository.openAsync().get(10, TimeUnit.SECONDS);
            Connection connection = portalRepository.getConnection();
            int currentVersion = portalRepository.getSchemaVersion(connection);
            portalRepository.ensureSchema(connection);
            if (currentVersion < 3) {
                migrateTaskIdsToPortalNameBased(connection);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Database migration interrupted: " + e.getMessage());
        } catch (TimeoutException e) {
            logger.warning("Database connection timed out after 10 seconds during migration: " + e.getMessage());
        } catch (ExecutionException | SQLException e) {
            logger.warning("Failed to run database migrations: " + e.getMessage());
        }
    }

    /**
     * Schema v3 migration: replace the random 128-char task IDs (two 64-char strings separated
     * by " <|||> ") with deterministic "{portalName}_particles <|||> {portalName}_trigger" values.
     *
     * <p>Task IDs are never used for task lookup at runtime (tasks are accessed by portalName),
     * so this is a cosmetic/housekeeping change that removes dead state from the DB.</p>
     */
    private void migrateTaskIdsToPortalNameBased(Connection connection) throws SQLException {
        // Only migrate rows whose taskIds look like the old random format (length >= 128 chars,
        // indicating two 64-char random strings joined by " <|||> ").
        String updateSql =
                "UPDATE PlayerPortalsTasks " +
                "SET taskIds = portal_Name || '_particles <|||> ' || portal_Name || '_trigger' " +
                "WHERE LENGTH(taskIds) >= 128";
        try (Statement stmt = connection.createStatement()) {
            int updated = stmt.executeUpdate(updateSql);
            if (updated > 0) {
                logger.info("[sRandomRTP] Migrated " + updated
                        + " portal task row(s) to deterministic task ID format (schema v3).");
            }
        }
    }

    /** Returns {@code true} if any key was actually added to {@code yaml}. */
    private boolean applyManagedDefaults(File file, YamlConfiguration yaml) {
        if (file == null || yaml == null) {
            return false;
        }

        boolean modified = false;
        String fileName = file.getName();
        if ("config.yml".equalsIgnoreCase(fileName)) {
            modified |= setIfMissing(yaml, "metrics.rtp.slow-request-threshold-ms", ConfigDefaults.SLOW_REQUEST_THRESHOLD_MS);
        } else if ("teleport.yml".equalsIgnoreCase(fileName)) {
            modified |= setIfMissing(yaml, "teleport.coordinate-generation",               ConfigDefaults.DEFAULT_COORDINATE_GENERATION);
            modified |= setIfMissing(yaml, "teleport.prefer-generated-chunks.enabled",     ConfigDefaults.PREFER_GENERATED_CHUNKS_ENABLED);
            modified |= setIfMissing(yaml, "teleport.prefer-generated-chunks.window-ms",   ConfigDefaults.PREFER_GENERATED_CHUNKS_WINDOW_MS);
            modified |= setIfMissing(yaml, "teleport.prefer-generated-chunks.max-attempts", ConfigDefaults.PREFER_GENERATED_CHUNKS_MAX_ATTEMPTS);
            modified |= setIfMissing(yaml, "teleport.parallel-search.enabled",             ConfigDefaults.PARALLEL_SEARCH_ENABLED);
            modified |= setIfMissing(yaml, "teleport.parallel-search.candidates-per-batch", ConfigDefaults.PARALLEL_SEARCH_CANDIDATES_PER_BATCH);
            modified |= setIfMissing(yaml, "teleport.parallel-search.max-global-inflight", ConfigDefaults.PARALLEL_SEARCH_MAX_GLOBAL_INFLIGHT);
        }
        return modified;
    }

    /** Sets {@code path} to {@code value} only if it is not already present. Returns {@code true} if the key was added. */
    private boolean setIfMissing(YamlConfiguration yaml, String path, Object value) {
        if (!yaml.contains(path)) {
            yaml.set(path, value);
            return true;
        }
        return false;
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
