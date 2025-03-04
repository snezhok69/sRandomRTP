package org.sRandomRTP.DataPortals;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;

public class SQLManagerPortals {

    public static CompletableFuture<Void> openConnectionSQL() {
        return CompletableFuture.runAsync(() -> {
            try {
                File pluginDir = Variables.getInstance().getDataFolder();
                if (!pluginDir.exists()) {
                    pluginDir.mkdirs();
                }

                String dbPath = pluginDir.getPath() + File.separator + "Portals.db";

                boolean isNewDatabase = !new File(dbPath).exists();

                Variables.connectionSQLPortal = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

                if (isNewDatabase) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §aСоздание новой базы данных...");
                    createTableSQL().get();
                }
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при создании/подключении к базе данных: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static CompletableFuture<Void> createTableSQL() {
        return CompletableFuture.runAsync(() -> {
            if (Variables.connectionSQLPortal == null) {
                try {
                    openConnectionSQL().get();
                } catch (Exception e) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cНе удалось подключиться к базе данных!");
                    return;
                }
            }

            try (Statement statement = Variables.connectionSQLPortal.createStatement()) {
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

                DatabaseMetaData metaData = Variables.connectionSQLPortal.getMetaData();

                ResultSet columnsBlocks = metaData.getColumns(null, null, "PlayerPortalsBlocks", "shape");
                if (!columnsBlocks.next()) {
                    statement.executeUpdate("ALTER TABLE PlayerPortalsBlocks ADD COLUMN shape TEXT DEFAULT 'circle'");
                }
                columnsBlocks.close();

                ResultSet columnsTasks = metaData.getColumns(null, null, "PlayerPortalsTasks", "shape");
                if (!columnsTasks.next()) {
                    statement.executeUpdate("ALTER TABLE PlayerPortalsTasks ADD COLUMN shape TEXT DEFAULT 'circle'");
                }
                columnsTasks.close();

                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §aСтруктура базы данных успешно создана/обновлена");

            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при создании таблиц: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public static Connection getConnectionSQL() {
        try {
            if (Variables.connectionSQLPortal == null || Variables.connectionSQLPortal.isClosed()) {
                File pluginDir = Variables.getInstance().getDataFolder();
                if (!pluginDir.exists()) {
                    pluginDir.mkdirs();
                }

                Class.forName("org.sqlite.JDBC");
                Variables.connectionSQLPortal = DriverManager.getConnection("jdbc:sqlite:" +
                        pluginDir.getPath() + File.separator + "Portals.db");
            }
            return Variables.connectionSQLPortal;
        } catch (ClassNotFoundException | SQLException e) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при подключении к базе данных: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static CompletableFuture<Void> closeConnectionMYSQL() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (Variables.connectionSQLPortal != null && !Variables.connectionSQLPortal.isClosed()) {
                    Variables.connectionSQLPortal.close();
                    Variables.connectionSQLPortal = null;
                }
            } catch (SQLException e) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при закрытии соединения с базой данных: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}