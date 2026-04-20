package org.sRandomRTP.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BootstrapCoordinator.FileChangeSummary} and database-init path.
 * Does not require a running Bukkit server.
 */
@Tag("unit")
class BootstrapCoordinatorTest {

    @TempDir
    Path tempDir;

    private PortalRepository repository;
    private MigrationRunner migrationRunner;

    @BeforeEach
    void setUp() {
        Logger logger = Logger.getLogger("BootstrapCoordinatorTest");
        ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());
        repository = new PortalRepository(tempDir.toFile(), logger);
        migrationRunner = new MigrationRunner(logger, configRegistry, repository);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (repository != null) {
            repository.closeAsync().get(5, TimeUnit.SECONDS);
        }
    }

    // ── FileChangeSummary contract ────────────────────────────────────────────

    @Test
    void successfulSummaryHasNullFailure() {
        BootstrapCoordinator.FileChangeSummary summary =
                new BootstrapCoordinator.FileChangeSummary(
                        Arrays.asList("a.yml"), Arrays.asList("b.yml"), null);
        assertTrue(summary.isSuccessful());
        assertNull(summary.getFailure());
        assertEquals(1, summary.getCreatedFiles().size());
        assertEquals(1, summary.getUpdatedFiles().size());
    }

    @Test
    void failedSummaryHasNonNullFailureAndIsNotSuccessful() {
        RuntimeException cause = new RuntimeException("file create failed");
        BootstrapCoordinator.FileChangeSummary summary =
                new BootstrapCoordinator.FileChangeSummary(
                        Collections.emptyList(), Collections.emptyList(), cause);
        assertFalse(summary.isSuccessful());
        assertNotNull(summary.getFailure());
        assertEquals(cause, summary.getFailure());
    }

    @Test
    void emptySummaryIsSuccessful() {
        BootstrapCoordinator.FileChangeSummary summary =
                new BootstrapCoordinator.FileChangeSummary(
                        Collections.emptyList(), Collections.emptyList(), null);
        assertTrue(summary.isSuccessful());
        assertTrue(summary.getCreatedFiles().isEmpty());
        assertTrue(summary.getUpdatedFiles().isEmpty());
    }

    // ── Database initialization (initializeDatabase delegates to MigrationRunner) ──

    @Test
    void initializeDatabaseCreatesCurrentSchema() throws Exception {
        // initializeDatabase() → migrationRunner.runDatabaseMigrations() → ensureSchema
        migrationRunner.runDatabaseMigrations();

        Connection connection = repository.getConnection();
        int version = repository.getSchemaVersion(connection);
        assertEquals(PluginVersionCatalog.PORTAL_SCHEMA_VERSION, version,
                "After runDatabaseMigrations, schema version must match PORTAL_SCHEMA_VERSION");
    }

    @Test
    void initializeDatabaseIsIdempotent() throws Exception {
        // Calling runDatabaseMigrations twice must not throw or corrupt the schema
        migrationRunner.runDatabaseMigrations();
        migrationRunner.runDatabaseMigrations();

        Connection connection = repository.getConnection();
        assertEquals(PluginVersionCatalog.PORTAL_SCHEMA_VERSION,
                repository.getSchemaVersion(connection),
                "Schema version must remain stable after repeated init calls");
    }
}
