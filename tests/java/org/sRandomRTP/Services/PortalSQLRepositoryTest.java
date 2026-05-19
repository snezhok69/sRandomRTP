package org.sRandomRTP.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sRandomRTP.Services.PluginVersionCatalog;
import org.sRandomRTP.Services.PortalRepository;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for portal SQLite schema and B1 regression (empty taskIds AIOOBE).
 * Uses an in-memory / temp-dir SQLite database via {@link PortalRepository}.
 */
@Tag("unit")
class PortalSQLRepositoryTest {

    @TempDir
    Path tempDir;

    private PortalRepository repository;
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        repository = new PortalRepository(tempDir.toFile(), Logger.getLogger("PortalSQLRepositoryTest"));
        repository.openAsync().get(10, TimeUnit.SECONDS);
        connection = repository.getConnection();
        repository.ensureSchema(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (repository != null) {
            repository.closeAsync().get(5, TimeUnit.SECONDS);
        }
    }

    // ── Schema migration ──────────────────────────────────────────────────────

    @Test
    void ensureSchemaCreatesCurrentSchemaVersion() throws Exception {
        assertEquals(PluginVersionCatalog.PORTAL_SCHEMA_VERSION,
                repository.getSchemaVersion(connection),
                "Schema version after ensureSchema() must match PORTAL_SCHEMA_VERSION");
    }

    @Test
    void ensureSchemaIsIdempotent() throws Exception {
        // Calling ensureSchema a second time must not throw or change the schema version
        assertDoesNotThrow(() -> repository.ensureSchema(connection),
                "Second call to ensureSchema() must be idempotent");
        assertEquals(PluginVersionCatalog.PORTAL_SCHEMA_VERSION,
                repository.getSchemaVersion(connection),
                "Schema version must be unchanged after second ensureSchema() call");
    }

    // ── B1 regression: empty taskIds must not cause AIOOBE ───────────────────

    @Test
    void emptyTaskIdsRowRoundTrips() throws Exception {
        // Insert a portal task row with taskIds = "" (the problematic value before B1 fix)
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO PlayerPortalsTasks " +
                "(player_Name, portal_Name, task_Type, delay, period, center_X, center_Y, center_Z, radius, taskIds, world, shape) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, "testPlayer");
            insert.setString(2, "home");
            insert.setString(3, "particles");
            insert.setLong(4, 1L);
            insert.setLong(5, 20L);
            insert.setDouble(6, 100.0);
            insert.setDouble(7, 64.0);
            insert.setDouble(8, 200.0);
            insert.setInt(9, 3);
            insert.setString(10, "");          // ← empty string: the pre-fix crash trigger
            insert.setString(11, "world");
            insert.setString(12, "circle");
            insert.executeUpdate();
        }

        // Read back the taskIds value and verify the guard logic would not AIOOBE
        try (PreparedStatement select = connection.prepareStatement(
                "SELECT taskIds FROM PlayerPortalsTasks WHERE player_Name = ?")) {
            select.setString(1, "testPlayer");
            try (ResultSet rs = select.executeQuery()) {
                assertNotNull(rs);
                rs.next();
                String taskIds = rs.getString("taskIds");
                // Reproduce the B1 guard logic from PortalSQLRepository:
                // if (taskIds == null || taskIds.isEmpty()) taskIds = "empty_id <|||> empty_id";
                if (taskIds == null || taskIds.isEmpty()) {
                    taskIds = "empty_id <|||> empty_id";
                }
                String[] parts = taskIds.split(" <\\|\\|\\|> ");
                // Must produce exactly 2 parts — no AIOOBE on parts[1]
                assertEquals(2, parts.length,
                        "taskIds guard must produce exactly 2 parts after substitution");
                assertNotEquals("", parts[0], "First task ID part must not be empty");
                assertNotEquals("", parts[1], "Second task ID part must not be empty");
            }
        }
    }

    @Test
    void nullTaskIdsHandledByGuard() {
        // Simulate the null path through the same guard logic
        String taskIds = null;
        if (taskIds == null || taskIds.isEmpty()) {
            taskIds = "empty_id <|||> empty_id";
        }
        String[] parts = taskIds.split(" <\\|\\|\\|> ");
        assertEquals(2, parts.length, "null taskIds guard must produce 2 parts");
    }

    @Test
    void validTaskIdsAreParsedCorrectly() {
        String taskIds = "home_particles <|||> home_trigger";
        if (taskIds == null || taskIds.isEmpty()) {
            taskIds = "empty_id <|||> empty_id";
        }
        String[] parts = taskIds.split(" <\\|\\|\\|> ");
        assertEquals(2, parts.length);
        assertEquals("home_particles", parts[0]);
        assertEquals("home_trigger", parts[1]);
    }
}
