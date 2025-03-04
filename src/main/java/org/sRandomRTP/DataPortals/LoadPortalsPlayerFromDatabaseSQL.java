package org.sRandomRTP.DataPortals;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.sRandomRTP.Commands.CommandSetPortal;
import org.sRandomRTP.DifferentMethods.Variables;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LoadPortalsPlayerFromDatabaseSQL {

    public static CompletableFuture<Void> loadPortalsPlayerFromDatabaseSQL() {
        return CompletableFuture.runAsync(() -> {
            try {
                Variables.connectionSQLPortal = SQLManagerPortals.getConnectionSQL();
                String selectSQL = "SELECT player_name, portal_names, portal_data FROM PlayerPortals";
                PreparedStatement selectStatement = Variables.connectionSQLPortal.prepareStatement(selectSQL);
                selectStatement.setFetchSize(Integer.MAX_VALUE);
                ResultSet resultSet = selectStatement.executeQuery();
                while (resultSet.next()) {
                    String playerName = resultSet.getString("player_name");
                    String portalNames = resultSet.getString("portal_names");
                    String portalData = resultSet.getString("portal_data");
                    String[] portals = portalNames.split(", ");
                    String[] data = portalData.split(" \\| ");
                    Map<String, PortalData> playerPortals = new HashMap<>();
                    for (int i = 0; i < portals.length; i++) {
                        if (i < data.length) {
                            String[] portalDetails = data[i].split(" -> ");
                            if (portalDetails.length == 2) {
                                String[] coordinates = portalDetails[0].split(", ");
                                String shape = "circle";
                                if (coordinates.length >= 5) {
                                    String worldName = coordinates[0];
                                    double x = Double.parseDouble(coordinates[1]);
                                    double y = Double.parseDouble(coordinates[2]);
                                    double z = Double.parseDouble(coordinates[3]);
                                    shape = coordinates[4];
                                    String portalNameWithIndex = portalDetails[1];
                                    PortalData playersDataPortals = new PortalData(playerName, worldName, portalNameWithIndex, String.valueOf(x), String.valueOf(y), String.valueOf(z), shape);
                                    playerPortals.put(portalNameWithIndex, playersDataPortals);
                                } else if (coordinates.length == 4) {
                                    String worldName = coordinates[0];
                                    double x = Double.parseDouble(coordinates[1]);
                                    double y = Double.parseDouble(coordinates[2]);
                                    double z = Double.parseDouble(coordinates[3]);
                                    String portalNameWithIndex = portalDetails[1];
                                    PortalData playersDataPortals = new PortalData(playerName, worldName, portalNameWithIndex, String.valueOf(x), String.valueOf(y), String.valueOf(z), shape);
                                    playerPortals.put(portalNameWithIndex, playersDataPortals);
                                }
                            }
                        }
                    }
                    Variables.playerPortals.put(playerName, playerPortals);
                }
                resultSet.close();
                selectStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void loadPortalBlocksPlayerToDatabaseSQL() {
        CompletableFuture.runAsync(() -> {
            try {
                String sql = "SELECT player_Name, portal_Name, block_Data, shape FROM PlayerPortalsBlocks";
                PreparedStatement statement = Variables.connectionSQLPortal.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String playerName = resultSet.getString("player_Name");
                    String portalName = resultSet.getString("portal_Name");
                    String blockData = resultSet.getString("block_Data");
                    String shape = "circle";
                    try {
                        shape = resultSet.getString("shape");
                        if (shape == null || shape.isEmpty()) {
                            shape = "circle";
                        }
                    } catch (SQLException e) {
                    }

                    String[] blocks = blockData.split(" \\| ");
                    for (String block : blocks) {
                        String[] parts = block.split(":");
                        String worldName = parts[0];
                        String[] coordinates = parts[1].split(",");
                        int x = Integer.parseInt(coordinates[0]);
                        int y = Integer.parseInt(coordinates[1]);
                        int z = Integer.parseInt(coordinates[2]);
                        Material material = Material.valueOf(parts[2]);
                        World world = Bukkit.getWorld(worldName);
                        if (world != null) {
                            Location location = new Location(world, x, y, z);
                            Block blockToSet = location.getBlock();
                            //blockToSet.setType(material);
                            String key = playerName + ":" + portalName + ":" + worldName + ":" + x + ":" + y + ":" + z;
                            Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(playerName, worldName, x, y, z, material.name(), portalName));
                        }
                    }
                }
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void loadPortalTasksFromDatabaseSQL() {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement statement = Variables.connectionSQLPortal.prepareStatement(
                        "SELECT player_Name, portal_Name, task_Type, delay, period, center_X, center_Y, center_Z, radius, taskIds, world, shape FROM PlayerPortalsTasks"
                );
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    String playerName = resultSet.getString("player_Name");
                    String portalName = resultSet.getString("portal_Name");
                    String taskType = resultSet.getString("task_Type");
                    long delay = resultSet.getLong("delay");
                    long period = resultSet.getLong("period");
                    double centerX = resultSet.getDouble("center_X");
                    double centerY = resultSet.getDouble("center_Y");
                    double centerZ = resultSet.getDouble("center_Z");
                    int radius = resultSet.getInt("radius");
                    String taskIds = resultSet.getString("taskIds");
                    String worldName = resultSet.getString("world");
                    String shape = "circle"; // По умолчанию круглый
                    try {
                        shape = resultSet.getString("shape");
                        if (shape == null || shape.isEmpty()) {
                            shape = "circle";
                        }
                    } catch (SQLException e) {
                        // Для обратной совместимости со старыми записями без столбца shape
                    }

                    // Проверка на null для taskIds
                    if (taskIds == null) {
                        taskIds = "empty_id <|||> empty_id"; // Предоставляем значение по умолчанию
                    }

                    try {
                        String[] taskIdsArray = taskIds.split(" <\\|\\|\\|> ");
                        String particlesTaskId = taskIdsArray[0];
                        String triggerTaskId = taskIdsArray.length > 1 ? taskIdsArray[1] : "empty_id";

                        World world = Bukkit.getWorld(worldName);
                        if (world == null) continue;

                        // Создаем неизменяемые копии переменных для использования в лямбда-выражениях
                        final Location center = new Location(world, centerX, centerY, centerZ);
                        final int finalRadius = radius;
                        final String finalShape = shape;

                        WrappedTask particlesTask = null;
                        WrappedTask triggerTask = null;
                        if (taskType.contains("particles")) {
                            particlesTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                                    () -> CommandSetPortal.spawnParticles(center, finalRadius, finalShape),
                                    delay,
                                    period
                            );
                        }
                        if (taskType.contains("trigger")) {
                            triggerTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                                    () -> CommandSetPortal.handlePortalTrigger(center, finalRadius, finalShape),
                                    delay,
                                    period
                            );
                        }

                        // Создаем новый строковый объект для taskIds, чтобы быть уверенными в его инициализации
                        String combinedTaskIds = particlesTaskId + " <|||> " + triggerTaskId;
                        Variables.playerPortalsTasks.put(portalName, new PortalDataTasks(playerName, portalName, taskType, delay, period, center, finalRadius, combinedTaskIds, particlesTask, triggerTask, finalShape));
                    } catch (Exception e) {
                        System.err.println("Ошибка при обработке taskIds: " + taskIds);
                        e.printStackTrace();
                    }
                }
                resultSet.close();
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }
}