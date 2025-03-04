package org.sRandomRTP.DataPortals;

import org.bukkit.Location;
import org.bukkit.World;
import org.sRandomRTP.DifferentMethods.Variables;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class SavePortalPlayerToDatabaseSQL {

    public static CompletableFuture<Void> savePortalPlayerToDatabaseSQL(String playerName, String worldName, String portalName, String x, String y, String z) {
        return savePortalPlayerToDatabaseSQL(playerName, worldName, portalName, x, y, z, "circle");
    }

    public static CompletableFuture<Void> savePortalPlayerToDatabaseSQL(String playerName, String worldName, String portalName, String x, String y, String z, String shape) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (Variables.connectionSQLPortal == null || Variables.connectionSQLPortal.isClosed()) {
                    Variables.connectionSQLPortal = SQLManagerPortals.getConnectionSQL();
                    if (Variables.connectionSQLPortal == null) {
                        System.err.println("Database connection is still null after attempting to establish connection.");
                        return;
                    }
                }

                SQLManagerPortals.createTableSQL().get();

                PreparedStatement checkStatement = Variables.connectionSQLPortal.prepareStatement(
                        "SELECT * FROM PlayerPortals WHERE player_name COLLATE BINARY = ?"
                );
                checkStatement.setString(1, playerName);
                ResultSet resultSet = checkStatement.executeQuery();
                if (resultSet.next()) {
                    String existingPortalNames = resultSet.getString("portal_names");
                    String existingPortalData = resultSet.getString("portal_data");
                    String[] portalNames = existingPortalNames.split(", ");
                    String newPortalName = portalName;
                    int index = 1;
                    while (Arrays.asList(portalNames).contains(newPortalName)) {
                        newPortalName = portalName + index;
                        index++;
                    }
                    String newPortalData = worldName + ", " + x + ", " + y + ", " + z + ", " + shape + " -> " + newPortalName;
                    existingPortalNames += ", " + newPortalName;
                    existingPortalData += " | " + newPortalData;
                    PreparedStatement updateStatement = Variables.connectionSQLPortal.prepareStatement(
                            "UPDATE PlayerPortals SET portal_names = ?, portal_data = ? WHERE player_name COLLATE BINARY = ?"
                    );
                    updateStatement.setString(1, existingPortalNames);
                    updateStatement.setString(2, existingPortalData);
                    updateStatement.setString(3, playerName);
                    updateStatement.executeUpdate();
                    updateStatement.close();
                } else {
                    String newPortalName = portalName;
                    String portalDataString = worldName + ", " + x + ", " + y + ", " + z + ", " + shape + " -> " + newPortalName;
                    PreparedStatement insertStatement = Variables.connectionSQLPortal.prepareStatement(
                            "INSERT INTO PlayerPortals (player_name, portal_names, portal_data) VALUES (?, ?, ?)"
                    );
                    insertStatement.setString(1, playerName);
                    insertStatement.setString(2, newPortalName);
                    insertStatement.setString(3, portalDataString);
                    insertStatement.executeUpdate();
                    insertStatement.close();
                }
                resultSet.close();
                checkStatement.close();
            } catch (SQLException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void savePortalBlocksPlayerToDatabaseSQL(String playerName, String portalName, int portalRadius, String allBlocksData) {
        savePortalBlocksPlayerToDatabaseSQL(playerName, portalName, portalRadius, allBlocksData, "circle");
    }

    public static void savePortalBlocksPlayerToDatabaseSQL(String playerName, String portalName, int portalRadius, String allBlocksData, String shape) {
        CompletableFuture.runAsync(() -> {
            try {
                String sql = "INSERT INTO PlayerPortalsBlocks (player_Name, portal_Name, portal_Radius, block_Data, shape) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement statement = Variables.connectionSQLPortal.prepareStatement(sql);
                statement.setString(1, playerName);
                statement.setString(2, portalName);
                statement.setInt(3, portalRadius);
                statement.setString(4, allBlocksData);
                statement.setString(5, shape);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void savePortalTasksToDatabaseSQL(String playerName, String portalName, String taskType, long delay, long period, Location center, int radius, String taskIds, World world) {
        savePortalTasksToDatabaseSQL(playerName, portalName, taskType, delay, period, center, radius, taskIds, world, "circle");
    }

    public static void savePortalTasksToDatabaseSQL(String playerName, String portalName, String taskType, long delay, long period, Location center, int radius, String taskIds, World world, String shape) {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = Variables.connectionSQLPortal.prepareStatement(
                        "INSERT INTO PlayerPortalsTasks (player_Name, portal_Name, task_Type, delay, period, center_X, center_Y, center_Z, radius, taskIds, world, shape) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
                statement.setString(1, playerName);
                statement.setString(2, portalName);
                statement.setString(3, taskType);
                statement.setLong(4, delay);
                statement.setLong(5, period);
                statement.setDouble(6, center.getX());
                statement.setDouble(7, center.getY());
                statement.setDouble(8, center.getZ());
                statement.setInt(9, radius);
                statement.setString(10, taskIds);
                statement.setString(11, world.getName());
                statement.setString(12, shape);
                statement.executeUpdate();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}