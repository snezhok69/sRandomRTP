package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationRunnerTest {

    @TempDir
    Path tempDir;

    private ConfigRegistry configRegistry;
    private PortalRepository repository;
    private MigrationRunner migrationRunner;

    @BeforeEach
    void setUp() {
        configRegistry = new ConfigRegistry(tempDir.toFile());
        repository = new PortalRepository(tempDir.toFile(), Logger.getLogger("MigrationRunnerTest"));
        migrationRunner = new MigrationRunner(Logger.getLogger("MigrationRunnerTest"), configRegistry, repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (repository != null) {
            repository.closeAsync().get();
        }
    }

    @Test
    void runConfigMigrationsAddsVersionMarker() throws IOException {
        File configFile = new File(tempDir.toFile(), "config.yml");
        configFile.getParentFile().mkdirs();
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("Language", "en");
        yaml.save(configFile);

        migrationRunner.runConfigMigrations();

        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(PluginVersionCatalog.CONFIG_VERSION, migrated.getInt("config-version"));
        assertEquals(ConfigDefaults.SLOW_REQUEST_THRESHOLD_MS, migrated.getLong("metrics.rtp.slow-request-threshold-ms"));
        assertEquals(ConfigDefaults.COMMAND_ALIASES, migrated.getStringList("Command-Aliases"));
    }

    @Test
    void runConfigMigrationsAddsTeleportPerformanceDefaults() throws IOException {
        File configFile = new File(tempDir.toFile(), "config.yml");
        File teleportFile = new File(tempDir.toFile(), "Settings/teleport.yml");
        configFile.getParentFile().mkdirs();
        teleportFile.getParentFile().mkdirs();
        YamlConfiguration configYaml = new YamlConfiguration();
        configYaml.set("Language", "en");
        configYaml.save(configFile);
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("teleport.maxtries", 1000);
        yaml.set("teleport.metrics.slow-request-threshold-ms", 4500L);
        yaml.save(teleportFile);

        migrationRunner.runConfigMigrations();

        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(teleportFile);
        YamlConfiguration migratedConfig = YamlConfiguration.loadConfiguration(configFile);
        assertEquals(PluginVersionCatalog.CONFIG_VERSION, migrated.getInt("config-version"));
        assertFalse(migrated.getBoolean("teleport.prefer-generated-chunks.enabled"));
        assertEquals(ConfigDefaults.PREFER_GENERATED_CHUNKS_WINDOW_MS, migrated.getLong("teleport.prefer-generated-chunks.window-ms"));
        assertEquals(ConfigDefaults.PREFER_GENERATED_CHUNKS_MAX_ATTEMPTS, migrated.getInt("teleport.prefer-generated-chunks.max-attempts"));
        assertFalse(migrated.getBoolean("teleport.parallel-search.enabled"));
        assertEquals(ConfigDefaults.PARALLEL_SEARCH_CANDIDATES_PER_BATCH, migrated.getInt("teleport.parallel-search.candidates-per-batch"));
        assertEquals(ConfigDefaults.PARALLEL_SEARCH_MAX_GLOBAL_INFLIGHT, migrated.getInt("teleport.parallel-search.max-global-inflight"));
        assertEquals(ConfigDefaults.DEFAULT_COORDINATE_GENERATION, migrated.getString("teleport.coordinate-generation"));
        assertEquals(4500L, migratedConfig.getLong("metrics.rtp.slow-request-threshold-ms"));
        assertFalse(migrated.contains("teleport.prefer-generated-chunks.invalid"));
    }

    @Test
    void runDatabaseMigrationsCreatesCurrentSchemaVersion() throws Exception {
        migrationRunner.runDatabaseMigrations();

        Connection connection = repository.getConnection();
        assertEquals(PluginVersionCatalog.PORTAL_SCHEMA_VERSION, repository.getSchemaVersion(connection));
        assertTrue(connection.isValid(2));
        // repository.closeAsync() is called in @AfterEach
    }
}
