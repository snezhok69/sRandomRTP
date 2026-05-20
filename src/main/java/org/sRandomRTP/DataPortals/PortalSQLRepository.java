package org.sRandomRTP.DataPortals;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.sRandomRTP.Commands.portal.PortalParticleManager;
import org.sRandomRTP.Commands.portal.PortalTriggerHandler;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import org.sRandomRTP.Utils.ConfigValueParser;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Consolidated portal database repository — replaces the four individual SQL classes:
 * SavePortalPlayerToDatabaseSQL, RemovePortalPlayerFromDatabaseSQL,
 * LoadPortalsPlayerFromDatabaseSQL, and RenameOrUpdatePortal.
 */
public class PortalSQLRepository {

    // ── Load ──────────────────────────────────────────────────────────────────

    public static CompletableFuture<Void> loadPortalsPlayerFromDatabaseSQL() {
        return SQLManagerPortals.runDbAsync("load portals from database", conn -> {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (state == null) return;
            state.getPlayerPortals().clear();
            try (PreparedStatement selectStatement = conn.prepareStatement(
                    "SELECT player_name, portal_names, portal_data FROM PlayerPortals")) {
                try (ResultSet resultSet = selectStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String playerName = resultSet.getString("player_name");
                        String portalNames = resultSet.getString("portal_names");
                        String portalData  = resultSet.getString("portal_data");
                        if (portalNames == null || portalNames.isEmpty()
                                || portalData == null || portalData.isEmpty()) {
                            if (playerName != null && !playerName.isEmpty()) {
                                Variables.getInstance().getLogger().warning(
                                        "[sRandomRTP] Skipping player '" + playerName
                                        + "': portal_names or portal_data is empty in database row");
                            }
                            continue;
                        }
                        String[] portals = PortalDataSerializer.parseNames(portalNames);
                        String[] data    = PortalDataSerializer.parseData(portalData);
                        if (portals.length != data.length) {
                            Variables.getInstance().getLogger().warning(
                                    "Portal data mismatch for player " + playerName
                                    + ": " + portals.length + " names vs " + data.length + " data entries — some portals may be skipped");
                        }
                        Map<String, PortalData> playerPortals = new HashMap<>();
                        for (int i = 0; i < portals.length; i++) {
                            if (i >= data.length) continue;
                            String[] portalDetails = data[i].split(" -> ");
                            if (portalDetails.length != 2) continue;
                            String[] coordinates = portalDetails[0].split(", ");
                            try {
                                if (coordinates.length < 4) continue;
                                String shape     = coordinates.length >= 5 ? coordinates[4] : "circle";
                                String worldName = coordinates[0];
                                double x = Double.parseDouble(coordinates[1]);
                                double y = Double.parseDouble(coordinates[2]);
                                double z = Double.parseDouble(coordinates[3]);
                                String portalNameWithIndex = portalDetails[1];
                                playerPortals.put(portalNameWithIndex, new PortalData(
                                        playerName, worldName, portalNameWithIndex, x, y, z, shape));
                            } catch (NumberFormatException e) {
                                Variables.getInstance().getLogger().warning(
                                        "Skipping corrupt portal row for player " + playerName + ": " + e.getMessage());
                            }
                        }
                        state.getPlayerPortals().put(playerName, playerPortals);
                    }
                }
            }
        });
    }

    public static CompletableFuture<Void> loadPortalBlocksPlayerToDatabaseSQL() {
        return SQLManagerPortals.runDbAsync("load portal blocks from database", conn -> {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (state == null) return;
            state.clearPortalBlocks();
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT player_Name, portal_Name, block_Data, shape FROM PlayerPortalsBlocks");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String playerName = resultSet.getString("player_Name");
                    String portalName = resultSet.getString("portal_Name");
                    String blockData  = resultSet.getString("block_Data");
                    if (playerName == null || portalName == null || blockData == null) {
                        Variables.getInstance().getLogger().warning(
                                "[sRandomRTP] Skipping portal block row with null player_Name, portal_Name, or block_Data");
                        continue;
                    }
                    for (String block : PortalDataSerializer.parseData(blockData)) {
                        try {
                            String[] parts = block.split(":");
                            if (parts.length < 3) {
                                Variables.getInstance().getLogger().warning(
                                        "Skipping malformed portal block (expected 3 colon-separated parts): " + block);
                                continue;
                            }
                            String worldName   = parts[0];
                            String[] coords    = parts[1].split(",");
                            if (coords.length < 3) {
                                Variables.getInstance().getLogger().warning(
                                        "Skipping malformed portal block coordinates: " + parts[1]);
                                continue;
                            }
                            int x = Integer.parseInt(coords[0]);
                            int y = Integer.parseInt(coords[1]);
                            int z = Integer.parseInt(coords[2]);
                            Material material = ConfigValueParser.parseMaterial(parts[2]);
                            if (material == null) {
                                Variables.getInstance().getLogger().warning(
                                        "Skipping portal block with unknown material: " + parts[2]);
                                continue;
                            }
                            World world = Bukkit.getWorld(worldName);
                            if (world != null) {
                                String key = playerName + ":" + portalName + ":" + worldName + ":" + x + ":" + y + ":" + z;
                                state.putPortalBlock(key, new PortalDataBlocks(playerName, worldName, x, y, z, material.name(), portalName));
                            }
                        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                            Variables.getInstance().getLogger().warning("Skipping corrupt portal block row: " + e.getMessage());
                        }
                    }
                }
            }
        });
    }

    public static CompletableFuture<Void> loadPortalTasksFromDatabaseSQL() {
        return SQLManagerPortals.runDbAsync("load portal tasks from database", conn -> {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            if (state == null) return;
            state.getPlayerPortalsTasks().clear();
            try (PreparedStatement statement = conn.prepareStatement(
                    "SELECT player_Name, portal_Name, task_Type, delay, period, center_X, center_Y, center_Z, radius, taskIds, world, shape FROM PlayerPortalsTasks");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String playerName = resultSet.getString("player_Name");
                    String portalName = resultSet.getString("portal_Name");
                    String taskType   = resultSet.getString("task_Type");
                    long   delay      = resultSet.getLong("delay");
                    long   period     = resultSet.getLong("period");
                    double centerX    = resultSet.getDouble("center_X");
                    double centerY    = resultSet.getDouble("center_Y");
                    double centerZ    = resultSet.getDouble("center_Z");
                    int    radius     = resultSet.getInt("radius");
                    String taskIds    = resultSet.getString("taskIds");
                    String worldName  = resultSet.getString("world");
                    String shape      = resultSet.getString("shape");

                    if (playerName == null || portalName == null || taskType == null || worldName == null) {
                        Variables.getInstance().getLogger().warning(
                                "[sRandomRTP] Skipping portal task row with null player_Name, portal_Name, task_Type, or world");
                        continue;
                    }
                    if (shape == null || shape.isEmpty()) shape = "circle";
                    // Guard against both null and empty string: "".split(...) produces [""],
                    // and taskIdsArray[1] would then throw ArrayIndexOutOfBoundsException.
                    if (taskIds == null || taskIds.isEmpty()) taskIds = "empty_id <|||> empty_id";

                    World world = Bukkit.getWorld(worldName);
                    if (world == null) continue;

                    try {
                        String[] taskIdsArray  = taskIds.split(" <\\|\\|\\|> ");
                        String particlesTaskId = taskIdsArray[0];
                        String triggerTaskId   = taskIdsArray.length > 1 ? taskIdsArray[1] : "empty_id";

                        final Location center     = new Location(world, centerX, centerY, centerZ);
                        final int    finalRadius  = radius;
                        final String finalShape   = shape;
                        final long   finalDelay   = delay;
                        final long   finalPeriod  = period;
                        WrappedTask particlesTask = null;
                        WrappedTask triggerTask   = null;
                        if (taskType.contains("particles")) {
                            particlesTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                                    () -> PortalParticleManager.spawnParticles(center, finalRadius, finalShape),
                                    finalDelay, finalPeriod);
                        }
                        if (taskType.contains("trigger")) {
                            PortalTriggerHandler triggerHandler = Variables.getPortalTriggerHandler();
                            if (triggerHandler != null) {
                                triggerTask = Variables.getFoliaLib().getImpl().runTimerAsync(
                                        () -> triggerHandler.handlePortalTrigger(center, finalRadius, finalShape),
                                        finalDelay, finalPeriod);
                            }
                        }
                        String combinedTaskIds = particlesTaskId + " <|||> " + triggerTaskId;
                        state.putPortalTask(portalName,
                                new PortalDataTasks(playerName, portalName, taskType, finalDelay, finalPeriod, center,
                                        finalRadius, combinedTaskIds, particlesTask, triggerTask, shape));
                    } catch (RuntimeException e) {
                        Variables.getInstance().getLogger().warning("Failed to process taskIds for portal '" + portalName + "': " + taskIds);
                    }
                }
            }
        });
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public static CompletableFuture<Void> savePortalPlayerToDatabaseSQL(String playerName, String worldName, String portalName, double x, double y, double z, String shape) {
        return SQLManagerPortals.runDbAsync("save portal data for " + playerName, conn ->
                SQLManagerPortals.upsertPortalData(conn, playerName, existing -> {
                    String newEntry = worldName + ", " + x + ", " + y + ", " + z + ", " + shape + " -> ";
                    if (existing != null) {
                        String[] names = PortalDataSerializer.parseNames(existing[0]);
                        Set<String> nameSet = new HashSet<>();
                        for (String n : names) nameSet.add(n);
                        String uniqueName = portalName;
                        int idx = 1;
                        while (nameSet.contains(uniqueName)) {
                            uniqueName = portalName + idx++;
                        }
                        return new String[]{
                                existing[0] + PortalDataSerializer.NAME_DELIMITER + uniqueName,
                                existing[1] + PortalDataSerializer.DATA_DELIMITER + newEntry + uniqueName
                        };
                    }
                    return new String[]{portalName, newEntry + portalName};
                })
        );
    }

    public static void savePortalBlocksPlayerToDatabaseSQL(String playerName, String portalName, int portalRadius, String allBlocksData, String shape) {
        SQLManagerPortals.fireAndForgetAsync("save portal blocks for " + playerName, conn -> {
            try (PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO PlayerPortalsBlocks (player_Name, portal_Name, portal_Radius, block_Data, shape) VALUES (?, ?, ?, ?, ?)")) {
                statement.setString(1, playerName);
                statement.setString(2, portalName);
                statement.setInt(3, portalRadius);
                statement.setString(4, allBlocksData);
                statement.setString(5, shape);
                statement.executeUpdate();
            }
        });
    }

    public static void savePortalTasksToDatabaseSQL(String playerName, String portalName, String taskType, long delay, long period, Location center, int radius, String taskIds, World world, String shape) {
        SQLManagerPortals.fireAndForgetAsync("save portal tasks for " + playerName, conn -> {
            try (PreparedStatement statement = conn.prepareStatement(
                    "INSERT INTO PlayerPortalsTasks (player_Name, portal_Name, task_Type, delay, period, center_X, center_Y, center_Z, radius, taskIds, world, shape) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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
            }
        });
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    public static CompletableFuture<Void> removePortalPlayerFromDatabaseSQL(String playerName, String portalName) {
        return SQLManagerPortals.runDbAsync("remove portal '" + portalName + "' for " + playerName, conn ->
                SQLManagerPortals.upsertPortalData(conn, playerName, existing -> {
                    if (existing == null) return existing;
                    String[] names = PortalDataSerializer.parseNames(existing[0]);
                    String[] data  = PortalDataSerializer.parseData(existing[1]);
                    StringBuilder sbNames = new StringBuilder();
                    StringBuilder sbData  = new StringBuilder();
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].equals(portalName)) continue;
                        if (sbNames.length() > 0) {
                            sbNames.append(PortalDataSerializer.NAME_DELIMITER);
                            sbData.append(PortalDataSerializer.DATA_DELIMITER);
                        }
                        sbNames.append(names[i]);
                        sbData.append(i < data.length ? data[i] : "");
                    }
                    return sbNames.length() == 0 ? null : new String[]{sbNames.toString(), sbData.toString()};
                })
        );
    }

    public static CompletableFuture<Void> removePortalBlocksPlayerToDatabaseSQL(String portalName) {
        return SQLManagerPortals.runDbAsync("remove portal blocks for '" + portalName + "'", conn -> {
            try (PreparedStatement deleteStatement = conn.prepareStatement(
                    "DELETE FROM PlayerPortalsBlocks WHERE portal_Name = ?")) {
                deleteStatement.setString(1, portalName);
                deleteStatement.executeUpdate();
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            List<PortalDataBlocks> removedBlocks = state.removePortalBlocksForPortal(portalName);
            for (PortalDataBlocks blockData : removedBlocks) {
                // Bukkit.getWorld() is safe on Paper's async thread; block modification
                // is deferred to the location thread via runAtLocation.
                World world = Bukkit.getWorld(blockData.getWorld());
                if (world != null) {
                    Location blockLocation = new Location(world, blockData.getX(), blockData.getY(), blockData.getZ());
                    state.getPlacedBlocks().remove(blockLocation); // ConcurrentHashMap — thread-safe
                    Variables.getFoliaLib().getImpl().runAtLocation(blockLocation, loc -> {
                        blockLocation.getBlock().setType(Material.AIR);
                    });
                }
            }
        });
    }

    public static CompletableFuture<Void> removePortalTasksFromDatabaseSQL(String portalName) {
        return SQLManagerPortals.runDbAsync("remove portal tasks for '" + portalName + "'", conn -> {
            try (PreparedStatement deleteStatement = conn.prepareStatement(
                    "DELETE FROM PlayerPortalsTasks WHERE portal_Name = ?")) {
                deleteStatement.setString(1, portalName);
                deleteStatement.executeUpdate();
            }
            RuntimeStateRegistry state = Variables.getRuntimeState();
            PortalDataTasks task = state.getPortalTask(portalName);
            if (task != null) {
                if (task.getParticlesTask() != null) task.getParticlesTask().cancel();
                if (task.getTriggerTask() != null) task.getTriggerTask().cancel();
                state.removePortalTask(portalName);
            }
        });
    }
}
