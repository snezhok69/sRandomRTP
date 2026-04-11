package org.sRandomRTP.Services;

import org.sRandomRTP.Main;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class PortalRepository {

    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }

    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    private final File dataFolder;
    private final Logger logger;
    private final ExecutorService dbExecutor;
    private final Object connectionLock = new Object();

    private volatile Connection connection;

    public PortalRepository(Main plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
    }

    public PortalRepository(File dataFolder, Logger logger) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.dbExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "sRandomRTP-portal-db");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<Void> openAsync() {
        return runAsync(new SqlConsumer() {
            @Override
            public void accept(Connection connection) {
            }
        });
    }

    public CompletableFuture<Void> ensureSchemaAsync() {
        return runAsync(new SqlConsumer() {
            @Override
            public void accept(Connection connection) throws SQLException {
                ensureSchema(connection);
            }
        });
    }

    public Connection getConnection() throws SQLException {
            synchronized (connectionLock) {
                if (connection == null || connection.isClosed()) {
                    ensureDriverLoaded();
                    if (!dataFolder.exists()) {
                        dataFolder.mkdirs();
                    }
                    String dbPath = dataFolder.getPath() + File.separator + "Portals.db";
                    connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                }
                return connection;
        }
    }

    public CompletableFuture<Void> runAsync(final SqlConsumer action) {
        return CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    action.accept(getConnection());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }, dbExecutor);
    }

    public <T> CompletableFuture<T> supplyAsync(final SqlFunction<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return action.apply(getConnection());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, dbExecutor);
    }

    public CompletableFuture<Void> closeAsync() {
        // Submit close task first (while executor still accepts work),
        // then shutdown — all previously queued tasks run before the close.
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            synchronized (connectionLock) {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        logger.warning("Failed to close portal database: " + e.getMessage());
                    } finally {
                        connection = null;
                    }
                }
            }
        }, dbExecutor);
        dbExecutor.shutdown();
        return future;
    }

    public void ensureSchema(Connection activeConnection) throws SQLException {
        if (activeConnection == null) {
            throw new SQLException("Portal database connection is not available");
        }

        try (Statement statement = activeConnection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS PlayerPortals ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_name TEXT NOT NULL,"
                    + "portal_names TEXT NOT NULL,"
                    + "portal_data TEXT NOT NULL,"
                    + "UNIQUE(player_name)"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS PlayerPortalsBlocks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_Name TEXT NOT NULL,"
                    + "portal_Name TEXT NOT NULL,"
                    + "portal_Radius TEXT NOT NULL,"
                    + "block_Data TEXT,"
                    + "shape TEXT DEFAULT 'circle'"
                    + ")");

            statement.executeUpdate("CREATE TABLE IF NOT EXISTS PlayerPortalsTasks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "player_Name TEXT NOT NULL,"
                    + "portal_Name TEXT NOT NULL,"
                    + "task_Type TEXT NOT NULL,"
                    + "delay BIGINT NOT NULL,"
                    + "period BIGINT NOT NULL,"
                    + "center_X DOUBLE NOT NULL,"
                    + "center_Y DOUBLE NOT NULL,"
                    + "center_Z DOUBLE NOT NULL,"
                    + "radius INTEGER NOT NULL,"
                    + "taskIds TEXT NOT NULL,"
                    + "world TEXT NOT NULL,"
                    + "shape TEXT DEFAULT 'circle'"
                    + ")");
        }

        ensureColumn(activeConnection, "PlayerPortalsBlocks", "shape",
                "ALTER TABLE PlayerPortalsBlocks ADD COLUMN shape TEXT DEFAULT 'circle'");
        ensureColumn(activeConnection, "PlayerPortalsTasks", "shape",
                "ALTER TABLE PlayerPortalsTasks ADD COLUMN shape TEXT DEFAULT 'circle'");
        setSchemaVersion(activeConnection, PluginVersionCatalog.PORTAL_SCHEMA_VERSION);
    }

    public int getSchemaVersion(Connection activeConnection) throws SQLException {
        try (PreparedStatement statement = activeConnection.prepareStatement("PRAGMA user_version");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        }
    }

    private void setSchemaVersion(Connection activeConnection, int version) throws SQLException {
        try (Statement statement = activeConnection.createStatement()) {
            statement.execute("PRAGMA user_version = " + version);
        }
    }

    private void ensureColumn(Connection activeConnection, String tableName, String columnName, String alterSql)
            throws SQLException {
        DatabaseMetaData metaData = activeConnection.getMetaData();
        try (ResultSet columns = metaData.getColumns(null, null, tableName, columnName)) {
            if (!columns.next()) {
                try (Statement statement = activeConnection.createStatement()) {
                    statement.executeUpdate(alterSql);
                }
            }
        }
    }

    private void ensureDriverLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.warning("SQLite JDBC driver is not available: " + e.getMessage());
        }
    }
}
