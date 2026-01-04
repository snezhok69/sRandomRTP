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
import org.sRandomRTP.DifferentMethods.Text.RandomStringGenerator;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpPortal;

import java.util.*;

public class CommandSetPortal {

    private static final List<Player> insidePlayers = new ArrayList<>();
    private static final HashMap<UUID, Long> teleportCooldown = new HashMap<>();

    private static Material getPortalFloorMaterial() {
        String materialName = Variables.portalfile.getString("portal.materials.floor", "GLASS");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Invalid material for portal floor in config: " + materialName + ". GLASS is used.");
            return Material.GLASS;
        }
    }

    private static Material getPortalBorderMaterial() {
        String materialName = Variables.portalfile.getString("portal.materials.border", "OBSIDIAN");
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Invalid material for portal border in config: " + materialName + ". OBSIDIAN is used.");
            return Material.OBSIDIAN;
        }
    }

    private static Particle getPortalFloorParticle() {
        String particleName = Variables.portalfile.getString("portal.particles.floor", "PORTAL");
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Incorrect particle type for portal floor in config: " + particleName + ". PORTAL is used.");
            return Particle.PORTAL;
        }
    }

    private static Particle getPortalBorderParticle() {
        String particleName = Variables.portalfile.getString("portal.particles.border", "FLAME");
        try {
            return Particle.valueOf(particleName.toUpperCase());
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Incorrect particle type for portal boundary in config: " + particleName + ". FLAME is used.");
            return Particle.FLAME;
        }
    }

    private static int getPortalFloorParticleCount() {
        return Variables.portalfile.getInt("portal.particles.floor_count");
    }

    private static int getPortalBorderParticleCount() {
        return Variables.portalfile.getInt("portal.particles.border_count");
    }

    private static double getPortalFloorParticleDensity() {
        return Variables.portalfile.getDouble("portal.particles.floor_density");
    }

    private static double getPortalBorderParticleDensity() {
        return Variables.portalfile.getDouble("portal.particles.border_density");
    }

    private static double getPortalFloorParticleSpread() {
        return Variables.portalfile.getDouble("portal.particles.floor_spread");
    }

    private static double getPortalBorderParticleSpread() {
        return Variables.portalfile.getDouble("portal.particles.border_spread");
    }

    private static boolean isPermanentBorderParticlesEnabled() {
        return Variables.portalfile.getBoolean("portal.particles.permanent_border");
    }

    private static boolean isPortalRtpEnabled() {
        return Variables.portalfile.getBoolean("portal.rtp.enabled");
    }

    private static String getPortalRtpWorld() {
        return Variables.portalfile.getString("portal.rtp.world");
    }

    public static boolean isPortalBlocksProtected() {
        return Variables.portalfile.getBoolean("portal.protect_blocks");
    }

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
            List<String> formattedMessage1 = LoadMessages.error_portal_shape_radius;
            for (String line : formattedMessage1) {
                line = line.replace("%shape%", shape).replace("%radius%", String.valueOf(radius));
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                sender.sendMessage(formattedLine);
            }
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
        Material floorMaterial = getPortalFloorMaterial();
        Material borderMaterial = getPortalBorderMaterial();

        if (shape.equals("circle")) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        Location glassLocation = center.clone().add(x, -1, z);
                        Block block = glassLocation.getBlock();
                        if (!Variables.placedBlocks.containsKey(glassLocation)) {
                            Variables.placedBlocks.put(glassLocation, block.getType());
                        }
                        block.setType(floorMaterial);

                        String blockData = world + ":" + glassLocation.getBlockX() + "," + glassLocation.getBlockY() + "," + glassLocation.getBlockZ() + ":" + floorMaterial.name();
                        if (allBlocksData.length() > 0) {
                            allBlocksData.append(" | ");
                        }
                        allBlocksData.append(blockData);

                        String key = player.getName() + ":" + portalName + ":" + world + ":" + glassLocation.getBlockX() + ":" + glassLocation.getBlockY() + ":" + glassLocation.getBlockZ();
                        Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ(), floorMaterial.name(), portalName));
                    }
                }
            }

            int x = 0;
            int z = radius;
            int d = 3 - 2 * radius;
            while (z >= x) {
                if (!isUnwantedBlock(x, z, radius)) {
                    placeObsidianBorder(center, x, z, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, -x, z, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, x, -z, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, -x, -z, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, z, x, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, -z, x, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, z, -x, player, portalName, world, allBlocksData, borderMaterial);
                    placeObsidianBorder(center, -z, -x, player, portalName, world, allBlocksData, borderMaterial);
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
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    Location glassLocation = center.clone().add(x, -1, z);
                    Block block = glassLocation.getBlock();
                    if (!Variables.placedBlocks.containsKey(glassLocation)) {
                        Variables.placedBlocks.put(glassLocation, block.getType());
                    }
                    block.setType(floorMaterial);

                    String blockData = world + ":" + glassLocation.getBlockX() + "," + glassLocation.getBlockY() + "," + glassLocation.getBlockZ() + ":" + floorMaterial.name();
                    if (allBlocksData.length() > 0) {
                        allBlocksData.append(" | ");
                    }
                    allBlocksData.append(blockData);

                    String key = player.getName() + ":" + portalName + ":" + world + ":" + glassLocation.getBlockX() + ":" + glassLocation.getBlockY() + ":" + glassLocation.getBlockZ();
                    Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, glassLocation.getBlockX(), glassLocation.getBlockY(), glassLocation.getBlockZ(), floorMaterial.name(), portalName));
                }
            }

            for (int x = -radius; x <= radius; x++) {
                placeObsidianBorder(center, x, -radius, player, portalName, world, allBlocksData, borderMaterial);
                placeObsidianBorder(center, x, radius, player, portalName, world, allBlocksData, borderMaterial);
            }
            for (int z = -radius + 1; z <= radius - 1; z++) {
                placeObsidianBorder(center, -radius, z, player, portalName, world, allBlocksData, borderMaterial);
                placeObsidianBorder(center, radius, z, player, portalName, world, allBlocksData, borderMaterial);
            }
        }

        SavePortalPlayerToDatabaseSQL.savePortalBlocksPlayerToDatabaseSQL(player.getName(), portalName, radius, allBlocksData.toString(), shape);
    }

    private static void placeObsidianBorder(Location center, int x, int z, Player player, String portalName, String world, StringBuilder allBlocksData, Material borderMaterial) {
        Location borderLocation = center.clone().add(x, 0, z);
        Block block = borderLocation.getBlock();
        if (!Variables.placedBlocks.containsKey(borderLocation)) {
            Variables.placedBlocks.put(borderLocation, block.getType());
        }
        block.setType(borderMaterial);

        String blockData = world + ":" + borderLocation.getBlockX() + "," + borderLocation.getBlockY() + "," + borderLocation.getBlockZ() + ":" + borderMaterial.name();
        if (allBlocksData.length() > 0) {
            allBlocksData.append(" | ");
        }
        allBlocksData.append(blockData);

        String key = player.getName() + ":" + portalName + ":" + world + ":" + borderLocation.getBlockX() + ":" + borderLocation.getBlockY() + ":" + borderLocation.getBlockZ();
        Variables.playerPortalsBlocks.put(key, new PortalDataBlocks(player.getName(), world, borderLocation.getBlockX(), borderLocation.getBlockY(), borderLocation.getBlockZ(), borderMaterial.name(), portalName));

        Particle borderParticle = getPortalBorderParticle();
        int particleCount = getPortalBorderParticleCount();
        double particleSpread = getPortalBorderParticleSpread();
        center.getWorld().spawnParticle(borderParticle, borderLocation.clone().add(0.5, 0.5, 0.5), particleCount, particleSpread, particleSpread, particleSpread, 0);
    }

    private static boolean isUnwantedBlock(int x, int z, int radius) {
        if (radius % 2 == 0 && radius <= 5) {
            return Math.abs(x) == Math.abs(z);
        }
        return false;
    }

    public static void spawnParticles(Location center, int radius, String shape) {
        Particle floorParticle = getPortalFloorParticle();
        int floorParticleCount = getPortalFloorParticleCount();
        double floorParticleYOffset = -0.5;
        double floorDensity = getPortalFloorParticleDensity();
        double floorSpread = getPortalFloorParticleSpread();

        if (shape.equals("circle")) {
            for (double x = -radius; x <= radius; x += floorDensity) {
                for (double z = -radius; z <= radius; z += floorDensity) {
                    if (x * x + z * z <= radius * radius) {
                        Location particleLocation = center.clone().add(x, floorParticleYOffset, z);
                        center.getWorld().spawnParticle(floorParticle, particleLocation, floorParticleCount, floorSpread, floorSpread, floorSpread, 0);
                    }
                }
            }
        } else if (shape.equals("square")) {
            for (double x = -radius; x <= radius; x += floorDensity) {
                for (double z = -radius; z <= radius; z += floorDensity) {
                    Location particleLocation = center.clone().add(x, floorParticleYOffset, z);
                    center.getWorld().spawnParticle(floorParticle, particleLocation, floorParticleCount, floorSpread, floorSpread, floorSpread, 0);
                }
            }
        }

        if (isPermanentBorderParticlesEnabled()) {
            spawnBorderParticles(center, radius, shape);
        }
    }

    private static void spawnBorderParticles(Location center, int radius, String shape) {
        Particle borderParticle = getPortalBorderParticle();
        int borderParticleCount = getPortalBorderParticleCount();
        double borderParticleYOffset = 0.0;
        double borderDensity = getPortalBorderParticleDensity();
        double borderSpread = getPortalBorderParticleSpread();

        if (shape.equals("circle")) {
            double increment = (2 * Math.PI) / (radius * (1 / borderDensity) * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location particleLocation = center.clone().add(x, borderParticleYOffset, z);
                center.getWorld().spawnParticle(borderParticle, particleLocation, borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        } else if (shape.equals("square")) {
            for (double x = -radius; x <= radius; x += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, -radius), borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, radius), borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
            for (double z = -radius + borderDensity; z < radius; z += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(-radius, borderParticleYOffset, z), borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(radius, borderParticleYOffset, z), borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        }
    }

    public static void handlePortalTrigger(Location center, int radius, String shape) {
        if (center == null || center.getWorld() == null) {
            return;
        }

        synchronized (insidePlayers) {
            List<Player> currentPlayers = new ArrayList<>();

            for (Player player : center.getWorld().getPlayers()) {
                if (player == null || !player.isOnline()) {
                    continue;
                }

                Location playerLocation = player.getLocation();
                if (isInsidePortal(playerLocation, center, radius, shape)) {
                    currentPlayers.add(player);

                    if (!insidePlayers.contains(player)) {
                        if (Variables.portalfile.getBoolean("portal.cooldown.enabled", true)) {
                            if (teleportCooldown.containsKey(player.getUniqueId())) {
                                long remainingTime = teleportCooldown.get(player.getUniqueId()) - System.currentTimeMillis();
                                if (remainingTime <= 0) {
                                    teleportCooldown.remove(player.getUniqueId());
                                } else {
                                    List<String> cooldownMessage = LoadMessages.error_cooldown_wait;
                                    if (cooldownMessage != null) {
                                        for (String line : cooldownMessage) {
                                            if (line != null) {
                                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                                player.sendMessage(formattedLine);
                                            }
                                        }
                                    }
                                    return;
                                }
                            }

                            if (isPortalRtpEnabled()) {
                                String targetWorldName = getPortalRtpWorld();
                                try {
                                    World targetWorld = Bukkit.getWorld(targetWorldName);
                                    if (targetWorld != null) {
                                        RtpRtpPortal.rtprtpportal(Bukkit.getConsoleSender(), player, targetWorld);
                                    } else {
                                        Bukkit.getLogger().warning("[sRandomRTP] Portal RTP world not found: " + targetWorldName);
                                    }
                                } catch (Exception e) {
                                    Bukkit.getLogger().warning("[sRandomRTP] Error teleporting player via portal RTP: " + e.getMessage());
                                }
                            }

                            if (Variables.portalfile != null && Variables.portalfile.getBoolean("portal-commands.enabled")) {
                                List<String> commands = Variables.portalfile.getStringList("portal-commands.commands");
                                if (commands != null) {
                                    for (String command : commands) {
                                        if (command != null && !command.isEmpty()) {
                                            final String finalCommand = command.replace("%player%", player.getName());
                                            Variables.getFoliaLib().getImpl().runAtEntity(player, (e) ->
                                                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
                                        }
                                    }
                                }
                            }
                            if (Variables.portalfile.getBoolean("portal.cooldown.enabled")) {
                                long cooldownSeconds = Variables.portalfile.getLong("portal.cooldown.time");
                                long cooldownMillis = cooldownSeconds * 1000;
                                teleportCooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMillis);
                            }
                        } else {
                            long remainingTime = teleportCooldown.get(player.getUniqueId()) - System.currentTimeMillis();
                            if (remainingTime <= 0) {
                                teleportCooldown.remove(player.getUniqueId());
                            } else {
                                List<String> cooldownMessage = LoadMessages.error_cooldown_wait;
                                if (cooldownMessage != null) {
                                    for (String line : cooldownMessage) {
                                        if (line != null) {
                                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                            player.sendMessage(formattedLine);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            insidePlayers.clear();
            insidePlayers.addAll(currentPlayers);
        }
    }

    private static boolean isInsidePortal(Location playerLocation, Location center, int radius, String shape) {
        double dx = playerLocation.getX() - center.getX();
        double dz = playerLocation.getZ() - center.getZ();
        double dy = playerLocation.getY() - center.getY();
        boolean isOnGlass = dy >= -1 && dy <= 0;

        if (shape.equals("circle")) {
            return isOnGlass && (dx * dx + dz * dz <= radius * radius);
        } else if (shape.equals("square")) {
            return isOnGlass && (Math.abs(dx) <= radius && Math.abs(dz) <= radius);
        }

        return false;
    }
}