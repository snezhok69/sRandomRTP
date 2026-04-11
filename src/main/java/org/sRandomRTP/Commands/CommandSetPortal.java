package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.DataPortals.PortalSQLRepository;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpSimple;
import org.sRandomRTP.Services.PortalSettings;
import org.sRandomRTP.Services.RuntimeStateRegistry;
import java.util.*;
import java.util.regex.Pattern;

public class CommandSetPortal {

    private static final Pattern PORTAL_NAME_PATTERN = Pattern.compile("^[a-zA-Zа-яА-ЯіїєІЇЄ0-9]+$");
    private static final Set<UUID> insidePlayers = new HashSet<>();
    private static final Object insideLock = new Object();
    private static final Map<UUID, Long> teleportCooldown = new java.util.concurrent.ConcurrentHashMap<>();

    public static boolean isPortalBlocksProtected() {
        return PortalSettings.current().isPortalBlocksProtected();
    }

    private static PortalSettings portalSettings() {
        return PortalSettings.current();
    }

    public static void commandSetPortal(CommandSender sender, int radius, String portalName, String shapeStr) {
        if (!CommandUtils.requirePlayer(sender).isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.PORTAL)) return;
        if (radius < 1 || radius > 10) {
            Variables.getMessageService().send(sender, LoadMessages.error_radius_portal);
            return;
        }
        if (!PORTAL_NAME_PATTERN.matcher(portalName).matches()) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name);
            return;
        }
        if (portalName.length() > 90) {
            int excessCharacters = portalName.length() - 90;
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name,
                    "%excessCharacters%", String.valueOf(excessCharacters));
            return;
        }
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state.hasPlayerPortal(sender.getName(), portalName)) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name_already_exists,
                    "%portalName%", portalName);
            return;
        }

        PortalShape shape = PortalShape.fromString(shapeStr);
        Player player = (Player) sender;
        Location center = player.getLocation().clone();
        center.setX(Math.floor(center.getX()) + 0.5);
        center.setZ(Math.floor(center.getZ()) + 0.5);
        center.setY(center.getY() - 1);

        if (!isAreaAvailable(center, radius, shape)) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_shape_radius,
                    "%shape%", shape.toString(), "%radius%", String.valueOf(radius));
            return;
        }

        createGlassAndBorder(center, radius, portalName, player, shape);

        WrappedTask particlesTask = Variables.getFoliaLib().getImpl().runTimerAsync(() ->
                spawnParticles(center, radius, shape), 0L, 10L);
        WrappedTask triggerTask = Variables.getFoliaLib().getImpl().runTimerAsync(() ->
                handlePortalTrigger(center, radius, shape), 0L, 20L);

        String particlesTaskId = generateRandomString(64);
        String triggerTaskId = generateRandomString(64);
        String taskIds = particlesTaskId + " <|||> " + triggerTaskId;
        World world = center.getWorld();
        if (world == null) return;
        PortalSQLRepository.savePortalTasksToDatabaseSQL(player.getName(), portalName,
                "trigger | particles", 0L, 20L, center, radius, taskIds, world, shape.toString());
        state.putPortalTask(portalName, new PortalDataTasks(player.getName(), portalName,
                "trigger | particles", 0L, 20L, center, radius, taskIds, particlesTask, triggerTask, shape.toString()));

        Variables.getMessageService().send(sender, LoadMessages.success_portal_created,
                "%portalName%", portalName, "%radius%", String.valueOf(radius), "%shape%", shape.toString());
    }

    private static boolean isAreaAvailable(Location center, int radius, PortalShape shape) {
        World world = center.getWorld();
        if (world == null) return false;
        int minX = world.getWorldBorder().getCenter().getBlockX() - (int) world.getWorldBorder().getSize() / 2;
        int maxX = world.getWorldBorder().getCenter().getBlockX() + (int) world.getWorldBorder().getSize() / 2;
        int minZ = world.getWorldBorder().getCenter().getBlockZ() - (int) world.getWorldBorder().getSize() / 2;
        int maxZ = world.getWorldBorder().getCenter().getBlockZ() + (int) world.getWorldBorder().getSize() / 2;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!shape.isCoordinateInFloor(x, z, radius)) continue;
                Location glassLocation = center.clone().add(x, -1, z);
                Block block = glassLocation.getBlock();
                if (glassLocation.getX() < minX || glassLocation.getX() > maxX
                        || glassLocation.getZ() < minZ || glassLocation.getZ() > maxZ) {
                    return false;
                }
                if (block.getType() != Material.AIR) {
                    return false;
                }
            }
        }
        if (shape == PortalShape.CIRCLE) {
            double increment = (2 * Math.PI) / (radius * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = center.getX() + (radius * Math.cos(angle));
                double z = center.getZ() + (radius * Math.sin(angle));
                Location borderLocation = new Location(world, x, center.getY(), z);
                Block block = borderLocation.getBlock();
                if (borderLocation.getX() < minX || borderLocation.getX() > maxX
                        || borderLocation.getZ() < minZ || borderLocation.getZ() > maxZ) {
                    return false;
                }
                if (block.getType() != Material.AIR) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void createGlassAndBorder(Location center, int radius, String portalName, Player player, PortalShape shape) {
        if (center.getWorld() == null) return;
        RuntimeStateRegistry state = Variables.getRuntimeState();
        String world = center.getWorld().getName();
        double xx = center.getX();
        double yy = center.getY();
        double zz = center.getZ();
        PortalData playerPortalMap = new PortalData(player.getName(), world, portalName, xx, yy, zz, shape.toString());
        state.putPlayerPortal(player.getName(), portalName, playerPortalMap);
        PortalSQLRepository.savePortalPlayerToDatabaseSQL(player.getName(), world, portalName, xx, yy, zz, shape.toString());

        StringBuilder allBlocksData = new StringBuilder();
        PortalSettings settings = portalSettings();
        Material floorMaterial = settings.getFloorMaterial();
        Material borderMaterial = settings.getBorderMaterial();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!shape.isCoordinateInFloor(x, z, radius)) continue;
                Location glassLocation = center.clone().add(x, -1, z);
                placePortalBlock(glassLocation, floorMaterial, player.getName(), portalName, world, allBlocksData, state);
            }
        }

        placeBorder(center, radius, player, portalName, world, allBlocksData, borderMaterial, settings, shape);
        PortalSQLRepository.savePortalBlocksPlayerToDatabaseSQL(player.getName(), portalName, radius, allBlocksData.toString(), shape.toString());
    }

    /**
     * Places a single portal block, records it in placedBlocks and the portal block registry,
     * and appends its data to the block data builder.
     */
    private static void placePortalBlock(Location loc, Material material, String playerName,
                                         String portalName, String world,
                                         StringBuilder allBlocksData, RuntimeStateRegistry state) {
        Block block = loc.getBlock();
        if (!state.getPlacedBlocks().containsKey(loc)) {
            state.getPlacedBlocks().put(loc, block.getType());
        }
        block.setType(material);

        String blockData = world + ":" + loc.getBlockX() + "," + loc.getBlockY() + ","
                + loc.getBlockZ() + ":" + material.name();
        if (allBlocksData.length() > 0) allBlocksData.append(" | ");
        allBlocksData.append(blockData);

        String key = playerName + ":" + portalName + ":" + world + ":"
                + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        state.putPortalBlock(key, new PortalDataBlocks(playerName, world,
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                material.name(), portalName));
    }

    private static void placeBorder(Location center, int radius, Player player, String portalName,
                                    String world, StringBuilder allBlocksData, Material borderMaterial,
                                    PortalSettings settings, PortalShape shape) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (shape == PortalShape.CIRCLE) {
            int x = 0;
            int z = radius;
            int d = 3 - 2 * radius;
            while (z >= x) {
                if (!isUnwantedBlock(x, z, radius)) {
                    placeObsidianBorder(center, x, z, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, -x, z, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, x, -z, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, -x, -z, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, z, x, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, -z, x, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, z, -x, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                    placeObsidianBorder(center, -z, -x, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                }
                if (d < 0) {
                    d = d + 4 * x + 6;
                } else {
                    d = d + 4 * (x - z) + 10;
                    z--;
                }
                x++;
            }
        } else { // SQUARE
            for (int bx = -radius; bx <= radius; bx++) {
                placeObsidianBorder(center, bx, -radius, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                placeObsidianBorder(center, bx, radius, player, portalName, world, allBlocksData, borderMaterial, settings, state);
            }
            for (int bz = -radius + 1; bz <= radius - 1; bz++) {
                placeObsidianBorder(center, -radius, bz, player, portalName, world, allBlocksData, borderMaterial, settings, state);
                placeObsidianBorder(center, radius, bz, player, portalName, world, allBlocksData, borderMaterial, settings, state);
            }
        }
    }

    private static void placeObsidianBorder(Location center, int x, int z, Player player, String portalName,
                                            String world, StringBuilder allBlocksData, Material borderMaterial,
                                            PortalSettings settings, RuntimeStateRegistry state) {
        Location borderLocation = center.clone().add(x, 0, z);
        placePortalBlock(borderLocation, borderMaterial, player.getName(), portalName, world, allBlocksData, state);

        Particle borderParticle = settings.getBorderParticle();
        int particleCount = settings.getBorderParticleCount();
        double particleSpread = settings.getBorderParticleSpread();
        if (center.getWorld() != null) {
            center.getWorld().spawnParticle(borderParticle, borderLocation.clone().add(0.5, 0.5, 0.5),
                    particleCount, particleSpread, particleSpread, particleSpread, 0);
        }
    }

    private static boolean isUnwantedBlock(int x, int z, int radius) {
        if (radius % 2 == 0 && radius <= 5) {
            return Math.abs(x) == Math.abs(z);
        }
        return false;
    }

    public static void spawnParticles(Location center, int radius, String shapeStr) {
        spawnParticles(center, radius, PortalShape.fromString(shapeStr));
    }

    public static void spawnParticles(Location center, int radius, PortalShape shape) {
        if (center == null || center.getWorld() == null) return;
        PortalSettings settings = portalSettings();
        Particle floorParticle = settings.getFloorParticle();
        int floorParticleCount = settings.getFloorParticleCount();
        double floorParticleYOffset = -0.5;
        double floorDensity = settings.getFloorParticleDensity();
        double floorSpread = settings.getFloorParticleSpread();

        for (double x = -radius; x <= radius; x += floorDensity) {
            for (double z = -radius; z <= radius; z += floorDensity) {
                if (!shape.isCoordinateInFloor((int) x, (int) z, radius)) continue;
                Location particleLocation = center.clone().add(x, floorParticleYOffset, z);
                center.getWorld().spawnParticle(floorParticle, particleLocation,
                        floorParticleCount, floorSpread, floorSpread, floorSpread, 0);
            }
        }

        if (settings.isPermanentBorderParticlesEnabled()) {
            spawnBorderParticles(center, radius, shape);
        }
    }

    private static void spawnBorderParticles(Location center, int radius, PortalShape shape) {
        PortalSettings settings = portalSettings();
        Particle borderParticle = settings.getBorderParticle();
        int borderParticleCount = settings.getBorderParticleCount();
        double borderParticleYOffset = settings.isPermanentBorderParticlesEnabled() ? 0.3 : -0.7;
        double borderDensity = settings.getBorderParticleDensity();
        double borderSpread = settings.getBorderParticleSpread();

        if (shape == PortalShape.CIRCLE) {
            double increment = (2 * Math.PI) / (radius * (1 / borderDensity) * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location particleLocation = center.clone().add(x, borderParticleYOffset, z);
                center.getWorld().spawnParticle(borderParticle, particleLocation,
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        } else { // SQUARE
            for (double x = -radius; x <= radius; x += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, -radius),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, radius),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
            for (double z = -radius + borderDensity; z < radius; z += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(-radius, borderParticleYOffset, z),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(radius, borderParticleYOffset, z),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        }
    }

    public static void handlePortalTrigger(Location center, int radius, String shapeStr) {
        handlePortalTrigger(center, radius, PortalShape.fromString(shapeStr));
    }

    public static void handlePortalTrigger(Location center, int radius, PortalShape shape) {
        if (center == null || center.getWorld() == null) return;
        PortalSettings settings = portalSettings();

        synchronized (insideLock) {
            Set<UUID> currentPlayers = new HashSet<>();

            for (Player player : center.getWorld().getPlayers()) {
                if (player == null || !player.isOnline()) continue;

                if (shape.isInside(player.getLocation(), center, radius)) {
                    currentPlayers.add(player.getUniqueId());

                    if (!insidePlayers.contains(player.getUniqueId())) {
                        if (settings.isCooldownEnabled() && isOnPortalCooldown(player)) {
                            Variables.getMessageService().send(player, LoadMessages.error_cooldown_wait);
                            continue;
                        }
                        triggerPortalEntry(player, settings);
                        if (settings.isCooldownEnabled()) {
                            long cooldownMillis = settings.getCooldownSeconds() * 1000L;
                            teleportCooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldownMillis);
                        }
                    }
                }
            }

            insidePlayers.clear();
            insidePlayers.addAll(currentPlayers);
        }
    }

    /**
     * Removes expired entries from the portal teleport cooldown map.
     * Should be called periodically from a background task to prevent unbounded growth.
     */
    public static void cleanExpiredPortalCooldowns() {
        long now = System.currentTimeMillis();
        teleportCooldown.entrySet().removeIf(e -> e.getValue() <= now);
    }

    /**
     * Cleans up all portal-related state for a player who has left the server.
     * Prevents unbounded growth of insidePlayers and teleportCooldown maps.
     */
    public static void handlePlayerQuit(UUID playerId) {
        synchronized (insideLock) {
            insidePlayers.remove(playerId);
        }
        teleportCooldown.remove(playerId);
    }

    private static boolean isOnPortalCooldown(Player player) {
        Long expiresAt = teleportCooldown.get(player.getUniqueId());
        if (expiresAt == null) return false;
        if (expiresAt <= System.currentTimeMillis()) {
            teleportCooldown.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    private static void triggerPortalEntry(Player player, PortalSettings settings) {
        if (settings.isPortalRtpEnabled()) {
            teleportThroughPortalRtp(player, settings.getPortalRtpWorld());
        }
        if (settings.isPortalCommandsEnabled()) {
            executePortalCommands(player, settings.getPortalCommands());
        }
    }

    private static void teleportThroughPortalRtp(Player player, String targetWorldName) {
        if (targetWorldName == null || targetWorldName.trim().isEmpty()) return;
        World targetWorld = Bukkit.getWorld(targetWorldName);
        if (targetWorld == null) {
            Bukkit.getLogger().warning("[sRandomRTP] Portal RTP world not found: " + targetWorldName);
            return;
        }
        try {
            RtpRtpSimple.launch(player, targetWorld, false, "[sRandomRTP-Portal]");
        } catch (RuntimeException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Error teleporting player via portal RTP: " + e.getMessage());
        }
    }

    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-=[]{}|;:,.<>?";

    private static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM_CHARS.charAt(java.util.concurrent.ThreadLocalRandom.current().nextInt(RANDOM_CHARS.length())));
        }
        return sb.toString();
    }

    private static void executePortalCommands(Player player, List<String> commands) {
        if (commands == null) return;
        for (String command : commands) {
            if (command == null || command.isEmpty()) continue;
            final String safeName = player.getName().replaceAll("[^a-zA-Z0-9_]", "");
            final String finalCommand = command.replace("%player%", safeName);
            Variables.getFoliaLib().getImpl().runAtEntity(player, e ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }
}
