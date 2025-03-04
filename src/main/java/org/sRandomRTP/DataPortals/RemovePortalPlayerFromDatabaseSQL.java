package org.sRandomRTP.DataPortals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.sRandomRTP.DifferentMethods.Variables;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RemovePortalPlayerFromDatabaseSQL {

    public static CompletableFuture<Void> removePortalPlayerFromDatabaseSQL(String playerName, String portalName) {
        return CompletableFuture.runAsync(() -> {
            try {
                Variables.connectionSQLPortal = SQLManagerPortals.getConnectionSQL();
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
                    String[] portalData = existingPortalData.split(" \\| ");
                    StringBuilder newPortalNames = new StringBuilder();
                    StringBuilder newPortalData = new StringBuilder();
                    boolean isFirst = true;
                    for (int i = 0; i < portalNames.length; i++) {
                        if (!portalNames[i].equals(portalName)) {
                            if (!isFirst) {
                                newPortalNames.append(", ");
                                newPortalData.append(" | ");
                            }
                            newPortalNames.append(portalNames[i]);
                            newPortalData.append(portalData[i]);
                            isFirst = false;
                        }
                    }
                    if (newPortalNames.length() == 0) {
                        PreparedStatement deleteStatement = Variables.connectionSQLPortal.prepareStatement(
                                "DELETE FROM PlayerPortals WHERE player_name COLLATE BINARY = ?"
                        );
                        deleteStatement.setString(1, playerName);
                        deleteStatement.executeUpdate();
                        deleteStatement.close();
                    } else {
                        PreparedStatement updateStatement = Variables.connectionSQLPortal.prepareStatement(
                                "UPDATE PlayerPortals SET portal_names = ?, portal_data = ? WHERE player_name COLLATE BINARY = ?"
                        );
                        updateStatement.setString(1, newPortalNames.toString());
                        updateStatement.setString(2, newPortalData.toString());
                        updateStatement.setString(3, playerName);
                        updateStatement.executeUpdate();
                        updateStatement.close();
                    }
                }
                resultSet.close();
                checkStatement.close();
            } catch (SQLException | ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    public static void removePortalBlocksPlayerToDatabaseSQL(String portalName) {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement deleteStatement = Variables.connectionSQLPortal.prepareStatement(
                        "DELETE FROM PlayerPortalsBlocks WHERE portal_Name = ?"
                );
                deleteStatement.setString(1, portalName);
                deleteStatement.executeUpdate();
                deleteStatement.close();
                Iterator<Map.Entry<String, PortalDataBlocks>> iterator = Variables.playerPortalsBlocks.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, PortalDataBlocks> entry = iterator.next();
                    if (entry.getValue().getPortalName().equals(portalName)) {
                        PortalDataBlocks blockData = entry.getValue();
                        World world = Bukkit.getWorld(blockData.getWorld());
                        if (world != null) {
                            Location blockLocation = new Location(world, blockData.getX(), blockData.getY(), blockData.getZ());
                            //
                            if (Bukkit.getServer().getName().equalsIgnoreCase("Folia")) {
                                Bukkit.getServer().getRegionScheduler().execute(Variables.getInstance(), (blockLocation), () -> {
                                    Block block = blockLocation.getBlock();
                                    block.setType(Material.AIR);
                                });
                            } else {
                                Variables.getFoliaLib().getImpl().runAtLocation(blockLocation, (loc) -> {
                                    Block block = blockLocation.getBlock();
                                    block.setType(Material.AIR);
                                });
                            }
                            //
                        }
                        iterator.remove();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void removePortalTasksFromDatabaseSQL(String portalName) {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement deleteStatement = Variables.connectionSQLPortal.prepareStatement(
                        "DELETE FROM PlayerPortalsTasks WHERE portal_Name = ?"
                );
                deleteStatement.setString(1, portalName);
                deleteStatement.executeUpdate();
                deleteStatement.close();
                PortalDataTasks task = Variables.playerPortalsTasks.get(portalName);
                if (task != null) {
                    if (task.getParticlesTask() != null) {
                        task.getParticlesTask().cancel();
                    }
                    if (task.getTriggerTask() != null) {
                        task.getTriggerTask().cancel();
                    }
                    Variables.playerPortalsTasks.remove(portalName);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}