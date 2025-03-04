package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.DataPortals.SavePortalPlayerToDatabaseSQL;
import org.sRandomRTP.DifferentMethods.RandomStringGenerator;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import java.util.*;

public class CommandSetPortal {

    private static final List<Player> insidePlayers = new ArrayList<>();
    private static final HashMap<UUID, Long> teleportCooldown = new HashMap<>();

    public static void commandSetPortal(CommandSender sender, int radius, String portalName, String shape) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Variables.pluginName + " §cOnly players can use this command!");
            return;
        }
        if (radius < 1 || radius > 10) {
            List<String> formattedMessage1 = LoadMessages.error_radius_portal;
            for (String line : formattedMessage1) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }
        String regex = "^[a-zA-Zа-яА-ЯіїєІЇЄ0-9]+$";
        if (!portalName.matches(regex)) {
            List<String> formattedMessage1 = LoadMessages.error_portal_name;
            for (String line : formattedMessage1) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }
        //
        if (portalName.length() > 90) {
            int excessCharacters = portalName.length() - 90;
            List<String> formattedMessage1 = LoadMessages.error_portal_name;
            for (String line : formattedMessage1) {
                line = line.replace("%excessCharacters%", String.valueOf(excessCharacters));
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }
        //
        Map<String, PortalData> playerPortals = Variables.playerPortals.get(sender.getName());
        if (playerPortals == null) {
            playerPortals = new HashMap<>();
            Variables.playerPortals.put(sender.getName(), playerPortals);
        }
        if (playerPortals.containsKey(portalName)) {
            List<String> formattedMessage1 = LoadMessages.error_portal_name_already_exists;
            for (String line : formattedMessage1) {
                line = line.replace("%portalName%", String.valueOf(portalName));
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
            return;
        }

        Player player = (Player) sender;
        Location center = player.getLocation().clone();
        center.setX(Math.floor(center.getX()) + 0.5);
        center.setZ(Math.floor(center.getZ()) + 0.5);
        center.setY(center.getY() - 1);

        if (!isAreaAvailable(center, radius, shape)) {
            sender.sendMessage(Variables.pluginName + " §cНедостаточно места для создания портала формы '" + shape + "' с радиусом " + radius + "!");
            return;
        }

        createGlassAndBorder(center, radius, portalName, player, shape);
        //
        WrappedTask particlesTask = Variables.getFoliaLib().getImpl().runTimerAsync(() ->
                spawnParticles(center, radius, shape), 0L, 10L);
        //
        WrappedTask triggerTask = Variables.getFoliaLib().getImpl().runTimerAsync(() ->
                handlePortalTrigger(center, radius, shape), 0L, 20L);
        //
        String particlesTaskId = RandomStringGenerator.generateRandomString(64);
        String triggerTaskId = RandomStringGenerator.generateRandomString(64);
        String taskIds = particlesTaskId + " <|||> " + triggerTaskId;
        World world = center.getWorld();
        SavePortalPlayerToDatabaseSQL.savePortalTasksToDatabaseSQL(player.getName(), portalName, "trigger | particles", 0L, 20L, center, radius, taskIds, world, shape);
        Variables.playerPortalsTasks.put(portalName, new PortalDataTasks(player.getName(), portalName, "trigger | particles", 0L, 20L, center, radius, taskIds, particlesTask, triggerTask, shape));

        List<String> successMessage = LoadMessages.success_portal_created;
        for (String line : successMessage) {
            line = line.replace("%portalName%", portalName).replace("%radius%", String.valueOf(radius)).replace("%shape%", shape);
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            sender.sendMessage(formattedLine);
        }
    }

    private static boolean isAreaAvailable(Location center, int radius, String shape) {
        World world = center.getWorld();
        int minX = world.getWorldBorder().getCenter().getBlockX() - (int) world.getWorldBorder().getSize() / 2;
        int maxX = world.getWorldBorder().getCenter().getBlockX() + (int) world.getWorldBorder().getSize() / 2;
        int minZ = world.getWorldBorder().getCenter().getBlockZ() - (int) world.getWorldBorder().getSize() / 2;
        int maxZ = world.getWorldBorder().getCenter().getBlockZ() + (int) world.getWorldBorder().getSize() / 2;

        if (shape.equals("circle")) {
            // Для круглого портала
            double increment = (2 * Math.PI) / (radius * 16);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Location glassLocation = center.clone().add(x, -1, z);
                        Block block = glassLocation.getBlock();
                        if (glassLocation.getX() < minX || glassLocation.getX() > maxX || glassLocation.getZ() < minZ || glassLocation.getZ() > maxZ) {
                            return false;
                        }
                        if (block.getType() != Material.AIR) {
                            return false;
                        }
                    }
                }
            }
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = center.getX() + (radius * Math.cos(angle));
                double z = center.getZ() + (radius * Math.sin(angle));
                Location borderLocation = new Location(world, x, center.getY(), z);
                Block block = borderLocation.getBlock();
                if (borderLocation.getX() < minX || borderLocation.getX() > maxX || borderLocation.getZ() < minZ || borderLocation.getZ() > maxZ) {
                    return false;
                }
                if (block.getType() != Material.AIR) {
                    return false;
                }
            }
        } else if (shape.equals("square")) {
            // Для квадратного портала
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location glassLocation = center.clone().add(x, -1, z);
                    Block block = glassLocation.getBlock();
                    if (glassLocation.getX() < minX || glassLocation.getX() > maxX || glassLocation.getZ() < minZ || glassLocation.getZ() > maxZ) {
                        return false;
                    }
                    if (block.getType() != Material.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void createGlassAndBorder(Location center, int radius, String portalName, Player player, String shape) {
        String world = center.getWorld().getName();
        String xx = String.valueOf(center.getX());
        String yy = String.valueOf(center.getY());
        String zz = String.valueOf(center.getZ());
        PortalData playerPortalMap = new PortalData(player.getName(), world, portalName, xx, yy, zz, shape);
        Map<String, PortalData> playerPortals = Variables.playerPortals.computeIfAbsent(player.getName(), k -> new HashMap<>());
        playerPortals.put(portalName, playerPortalMap);
        SavePortalPlayerToDatabaseSQL.savePortalPlayerToDatabaseSQL(player.getName(), world, portalName, xx, yy, zz, shape);
        //
        StringBuilder allBlocksData = new StringBuilder();

        if (shape.equals("circle")) {
            // Круглый портал - основание
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Location glassLocation = center.clone().add(x, -1, z);
                        Block block = glassLocation.getBlock();
                        if (!Variables.placedBlocks.containsKey(glassLocation)) {
                            Variables.placedBlocks.put(glassLocation, block.getType());
                        }
                        block.setType(Material.GLASS);

                        String blockData = world + ":" + glassLocation.getBlockX() + "," + glassLocation.getBlockY() + "," + glassLocation.getBlockZ() + ":" + Material.GLASS.name();
                        if (allBlocksData.length() > 0) {
                            allBlocksData.append(" | ");
                        }
                        allBlocksData.append(blockData);

                        String key = player.getName() + ":" + portalName + ":" + world + ":" + glassLocation.getBlockX() + ":" + glassLocation.getBlockY() + ":" + glassLocation.getBlockZ();
                        Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ(), Material.GLASS.name(), portalName));
                    }
                }
            }

            // Круглый портал - граница
            int x = 0;
            int z = radius;
            int d = 3 - 2 * radius;
            while (z >= x) {
                if (!isUnwantedBlock(x, z, radius)) {
                    placeObsidianBorder(center, x, z, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, -x, z, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, x, -z, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, -x, -z, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, z, x, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, -z, x, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, z, -x, player, portalName, world, allBlocksData);
                    placeObsidianBorder(center, -z, -x, player, portalName, world, allBlocksData);
                }
                if (d < 0) {
                    d = d + 4 * x + 6;
                } else {
                    d = d + 4 * (x - z) + 10;
                    z--;
                }
                x++;
            }
        } else if (shape.equals("square")) {
            // Квадратный портал - основание
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location glassLocation = center.clone().add(x, -1, z);
                    Block block = glassLocation.getBlock();
                    if (!Variables.placedBlocks.containsKey(glassLocation)) {
                        Variables.placedBlocks.put(glassLocation, block.getType());
                    }
                    block.setType(Material.GLASS);

                    String blockData = world + ":" + glassLocation.getBlockX() + "," + glassLocation.getBlockY() + "," + glassLocation.getBlockZ() + ":" + Material.GLASS.name();
                    if (allBlocksData.length() > 0) {
                        allBlocksData.append(" | ");
                    }
                    allBlocksData.append(blockData);

                    String key = player.getName() + ":" + portalName + ":" + world + ":" + glassLocation.getBlockX() + ":" + glassLocation.getBlockY() + ":" + glassLocation.getBlockZ();
                    Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ(), Material.GLASS.name(), portalName));
                }
            }

            // Квадратный портал - граница
            for (int x = -radius; x <= radius; x++) {
                placeObsidianBorder(center, x, -radius, player, portalName, world, allBlocksData); // Север
                placeObsidianBorder(center, x, radius, player, portalName, world, allBlocksData);  // Юг
            }
            for (int z = -radius + 1; z <= radius - 1; z++) {
                placeObsidianBorder(center, -radius, z, player, portalName, world, allBlocksData); // Запад
                placeObsidianBorder(center, radius, z, player, portalName, world, allBlocksData);  // Восток
            }
        }

        SavePortalPlayerToDatabaseSQL.savePortalBlocksPlayerToDatabaseSQL(player.getName(), portalName, radius, allBlocksData.toString(), shape);
    }

    private static void placeObsidianBorder(Location center, int x, int z, Player player, String portalName, String world, StringBuilder allBlocksData) {
        Location borderLocation = center.clone().add(x, 0, z);
        Block block = borderLocation.getBlock();
        if (!Variables.placedBlocks.containsKey(borderLocation)) {
            Variables.placedBlocks.put(borderLocation, block.getType());
        }
        block.setType(Material.OBSIDIAN);

        String blockData = world + ":" + borderLocation.getBlockX() + "," + borderLocation.getBlockY() + "," + borderLocation.getBlockZ() + ":" + Material.OBSIDIAN.name();
        if (allBlocksData.length() > 0) {
            allBlocksData.append(" | ");
        }
        allBlocksData.append(blockData);

        String key = player.getName() + ":" + portalName + ":" + world + ":" + borderLocation.getBlockX() + ":" + borderLocation.getBlockY() + ":" + borderLocation.getBlockZ();
        Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, borderLocation.getBlockX(), borderLocation.getBlockY(), borderLocation.getBlockZ(), Material.OBSIDIAN.name(), portalName));

        center.getWorld().spawnParticle(Particle.FLAME, borderLocation.clone().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0);
    }

    private static boolean isUnwantedBlock(int x, int z, int radius) {
        if (radius % 2 == 0 && radius <= 5) {
            return Math.abs(x) == Math.abs(z);
        }
        return false;
    }

    public static void spawnParticles(Location center, int radius, String shape) {
        double increment = 0.5;

        if (shape.equals("circle")) {
            // Частицы для круглого портала
            for (double x = -radius; x <= radius; x += increment) {
                for (double z = -radius; z <= radius; z += increment) {
                    if (x * x + z * z <= radius * radius) {
                        Location particleLocation = center.clone().add(x, 0, z);
                        center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 2, 0.1, 0.1, 0.1, 0);
                    }
                }
            }
        } else if (shape.equals("square")) {
            // Частицы для квадратного портала
            for (double x = -radius; x <= radius; x += increment) {
                for (double z = -radius; z <= radius; z += increment) {
                    Location particleLocation = center.clone().add(x, 0, z);
                    center.getWorld().spawnParticle(Particle.PORTAL, particleLocation, 2, 0.1, 0.1, 0.1, 0);
                }
            }
        }
    }

    public static void handlePortalTrigger(Location center, int radius, String shape) {
        for (Player player : center.getWorld().getPlayers()) {
            Location playerLocation = player.getLocation();
            if (isInsidePortal(playerLocation, center, radius, shape)) {
                if (!insidePlayers.contains(player)) {
                    insidePlayers.add(player);
                    if (!teleportCooldown.containsKey(player.getUniqueId())) {
                        Variables.getFoliaLib().getImpl().runAtEntity(player, (e) -> player.performCommand("rtp"));
                        teleportCooldown.put(player.getUniqueId(), System.currentTimeMillis() + 5000);
                    } else {
                        long remainingTime = teleportCooldown.get(player.getUniqueId()) - System.currentTimeMillis();
                        if (remainingTime <= 0) {
                            teleportCooldown.remove(player.getUniqueId());
                        } else {
                            List<String> cooldownMessage = LoadMessages.error_cooldown_wait;
                            for (String line : cooldownMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                player.sendMessage(formattedLine);
                            };
                        }
                    }
                }
            } else {
                insidePlayers.remove(player);
            }
        }
    }

    private static boolean isInsidePortal(Location playerLocation, Location center, int radius, String shape) {
        double dx = playerLocation.getX() - center.getX();
        double dz = playerLocation.getZ() - center.getZ();
        double dy = playerLocation.getY() - center.getY();
        boolean isOnGlass = dy >= -1 && dy <= 0;

        if (shape.equals("circle")) {
            // Для круглого портала
            return isOnGlass && (dx * dx + dz * dz <= radius * radius);
        } else if (shape.equals("square")) {
            // Для квадратного портала
            return isOnGlass && (Math.abs(dx) <= radius && Math.abs(dz) <= radius);
        }

        return false;
    }
}