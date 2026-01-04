package org.sRandomRTP.Rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.BlockBiomes.IsBiomeBanned;
import org.sRandomRTP.BlockBiomes.IsBlockBanned;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.Teleport.GetChunksToLoad;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportWithChunkLoading;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import static org.sRandomRTP.DifferentMethods.Variables.pluginName;
import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.*;
import static org.sRandomRTP.DifferentMethods.rtprtps.HandleFailedAttempt.handleFailedAttempt;
import static org.sRandomRTP.DifferentMethods.rtprtps.ResolveTargetWorld.resolveTargetWorld;
import static org.sRandomRTP.DifferentMethods.rtprtps.SendLoadingFeedback.sendLoadingFeedback;

public class RtpRtpBase {
    public static void rtpRtpbase(CommandSender sender, World targetWorld) {
        Player player = sender instanceof Player ? (Player) sender : null;
        boolean searchStarted = false;
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage(pluginName + " §8- §cOnly players can use this command.");
                return;
            }

            player = (Player) sender;

            if (!player.isOnline()) {
                return;
            }

            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);

            World world = resolveTargetWorld(player, targetWorld, loggingEnabled);
            if (world == null) {
                player.sendMessage(pluginName + " §8- §cUnable to determine a valid world for teleportation.");
                return;
            }

            sendLoadingFeedback(player);

            Variables.initialPositions.put(player, player.getLocation());

            TeleportRequestContext context = TeleportRequestManager.beginRequest(player, loggingEnabled);
            searchStarted = true;

            Variables.playerSearchStatus.put(player.getName(), true);

            int maxAttempts = Math.max(1, Variables.teleportfile.getInt("teleport.maxtries"));

            scheduleNextAttempt(player, world, maxAttempts, loggingEnabled, config, context, 0L);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        } finally {
            if (!searchStarted) {
                EconomyPaymentManager.refund(player);
            }
        }
    }

    private static void scheduleNextAttempt(Player player, World world, int maxAttempts, boolean loggingEnabled,
                                            FileConfiguration config, TeleportRequestContext context, long delayTicks) {
        if (!player.isOnline()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("Skipping teleport search for player " + player.getName() + " because they are offline.");
            }
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "player offline");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        if (context.isCancelled() || context.isCompleted()) {
            return;
        }

        if (context.isExpired()) {
            handleTimedOutSearch(player, loggingEnabled);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "request expired");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        WrappedTask task = Variables.getFoliaLib().getImpl().runLater(() ->
                        attemptCoordinate(player, world, maxAttempts, loggingEnabled, config, context),
                Math.max(delayTicks, 0L));
        TeleportRequestManager.registerTask(player, task);
    }

    private static void attemptCoordinate(Player player, World world, int maxAttempts, boolean loggingEnabled,
                                          FileConfiguration config, TeleportRequestContext context) {
        if (!player.isOnline()) {
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "player offline");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        if (context.isCancelled() || context.isCompleted()) {
            return;
        }

        if (context.isExpired()) {
            handleTimedOutSearch(player, loggingEnabled);
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "request expired");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        int attemptNumber = context.incrementAttempt();

        if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
            Bukkit.getConsoleSender().sendMessage("Starting teleport attempt #" + attemptNumber + " for player " + player.getName());
        }

        Runnable retryCallback = () -> scheduleNextAttempt(player, world, maxAttempts, loggingEnabled, config, context, 1L);

        if (attemptNumber > maxAttempts) {
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        // Специфика RtpRtpBase - генерация координат на основе регионов WorldGuard
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("RegionManager is null for world: " + world.getName());
            }
            player.sendMessage(pluginName + " §8- §cError getting regions. Please check logs.");
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no region manager");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        List<ProtectedRegion> regions = new ArrayList<>(regionManager.getRegions().values());
        regions.removeIf(region -> region.getId().equalsIgnoreCase("__global__"));

        if (regions.isEmpty()) {
            List<String> formattedMessage2 = LoadMessages.regionsempty;
            for (String line : formattedMessage2) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                player.sendMessage(formattedLine);
            }
            TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "no regions");
            Variables.playerSearchStatus.put(player.getName(), false);
            return;
        }

        ProtectedRegion randomRegion = regions.get(new Random().nextInt(regions.size()));
        BlockVector3 min = randomRegion.getMinimumPoint();
        BlockVector3 max = randomRegion.getMaximumPoint();
        int regionradius = Variables.teleportfile.getInt("teleport.regionradius");

        int centerX = (int) world.getWorldBorder().getCenter().getX();
        int centerZ = (int) world.getWorldBorder().getCenter().getZ();

        int newX, newZ;
        Random random = new Random();
        boolean offsetXPositive = random.nextBoolean();
        boolean offsetZPositive = random.nextBoolean();

        if (offsetXPositive) {
            newX = max.getBlockX() + 1 + random.nextInt(regionradius);
        } else {
            newX = min.getBlockX() - 1 - random.nextInt(regionradius);
        }

        if (offsetZPositive) {
            newZ = max.getBlockZ() + 1 + random.nextInt(regionradius);
        } else {
            newZ = min.getBlockZ() - 1 - random.nextInt(regionradius);
        }

        if (newX == 0 && newZ == 0) {
            if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber + " failed due to unsafe location (coordinates 0,0).");
            }
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        final int finalNewX = newX;
        final int finalNewZ = newZ;

        if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
            double distance = Math.sqrt(Math.pow(finalNewX - centerX, 2) + Math.pow(finalNewZ - centerZ, 2));
            Bukkit.getConsoleSender().sendMessage("Generated coordinates near region: X=" + finalNewX + ", Z=" + finalNewZ + ", Distance from center: " + (int) distance + " blocks");
        }

        Location targetLocation = new Location(world, finalNewX, 0, finalNewZ);
        if (!world.getWorldBorder().isInside(targetLocation)) {
            if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: "
                        + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ());
            }
            if (attemptNumber >= maxAttempts) {
                for (String line : LoadMessages.worldborder_error) {
                    player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                }
            }
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        CandidateSearchTask task = createCandidateSearchTask(player, world, finalNewX, finalNewZ, loggingEnabled, attemptNumber, context);
        if (task == null) {
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        AtomicBoolean attemptResolved = new AtomicBoolean(false);

        CompletableFuture<Void> attemptFuture = new CompletableFuture<>();
        context.trackFuture(attemptFuture);
        attemptFuture.whenComplete((ignored, throwable) -> {
            if (throwable != null) {
                if (throwable instanceof CancellationException || context.isCancelled() || context.isCompleted()) {
                    return;
                }
                if (attemptResolved.compareAndSet(false, true)) {
                    if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                        Bukkit.getLogger().warning("Teleport attempt #" + attemptNumber + " timed out for player " + player.getName());
                    }
                    handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                }
            }
        });

        task.future.whenComplete((candidate, throwable) -> {
            if (context.isCancelled() || context.isCompleted()) {
                attemptFuture.complete(null);
                return;
            }

            if (throwable != null) {
                if (loggingEnabled) {
                    Bukkit.getLogger().log(Level.SEVERE, "Error while processing Y-coordinate for player " + player.getName(), throwable);
                }
                attemptFuture.complete(null);
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }

            boolean success = candidate != null && candidate.y != -1;
            if (success) {
                attemptResolved.set(true);
                attemptFuture.complete(null);

                if (context.isCancelled() || context.isCompleted()) {
                    return;
                }

                Location processingLocation = new Location(world, task.x, Math.max(candidate.y, world.getMinHeight()), task.z);
                Variables.getFoliaLib().getImpl().runAtLocation(processingLocation, processingTask -> {
                    TeleportRequestManager.registerTask(player, processingTask);
                    processCandidateLocation(player, world, task.x, task.z, candidate, loggingEnabled, config, attemptNumber, maxAttempts, retryCallback, context);
                });
                return;
            }

            if (candidate != null && candidate.y == -1 && loggingEnabled && shouldLogAttempt(attemptNumber)) {
                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber + " failed due to unsafe location.");
            }

            if (attemptResolved.compareAndSet(false, true)) {
                attemptFuture.complete(null);
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            } else {
                attemptFuture.complete(null);
            }
        });
    }

    private static CandidateSearchTask createCandidateSearchTask(Player player, World world, int finalNewX, int finalNewZ,
                                                                 boolean loggingEnabled, int attemptNumber,
                                                                 TeleportRequestContext context) {
        if (context.isCancelled() || context.isCompleted()) {
            return new CandidateSearchTask(finalNewX, finalNewZ, CompletableFuture.completedFuture(null));
        }

        CompletableFuture<CoordinateCandidate> yCoordinateFuture;
        if (world.getEnvironment() == World.Environment.NETHER) {
            yCoordinateFuture = GetSafeYCoordinateInNether.getSafeYCoordinateInNetherAsync(world, finalNewX, finalNewZ, context)
                    .thenApply(y -> new CoordinateCandidate(y, null));
        } else if (world.getEnvironment() == World.Environment.THE_END) {
            yCoordinateFuture = GetSafeYCoordinateInEnd.getSafeYCoordinateInEndAsync(world, finalNewX, finalNewZ, context)
                    .thenApply(y -> new CoordinateCandidate(y, null));
        } else {
            yCoordinateFuture = GetSafeYCoordinate.getSafeYCoordinateWithAirCheckAsync(world, finalNewX, finalNewZ, context)
                    .thenApply(coord -> coord == null ? new CoordinateCandidate(-1, null)
                            : new CoordinateCandidate(coord.y, coord.biome));
        }

        return new CandidateSearchTask(finalNewX, finalNewZ, yCoordinateFuture);
    }

    private static void processCandidateLocation(Player player, World world, int finalNewX, int finalNewZ,
                                                 CoordinateCandidate candidate, boolean loggingEnabled, FileConfiguration config,
                                                 int attemptNumber, int maxAttempts, Runnable retryCallback, TeleportRequestContext context) {
        int newY = candidate.y;
        Biome targetBiome = candidate.biome != null ? candidate.biome : world.getBiome(finalNewX, newY, finalNewZ);

        Block targetBlock = world.getBlockAt(finalNewX, newY - 1, finalNewZ);
        Block blockAbove = world.getBlockAt(finalNewX, newY, finalNewZ);
        Block blockTwoAbove = world.getBlockAt(finalNewX, newY + 1, finalNewZ);

        if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
            Bukkit.getConsoleSender().sendMessage("Trying teleport to: X=" + finalNewX + ", Y=" + newY + ", Z=" + finalNewZ);
            Bukkit.getConsoleSender().sendMessage("Target block: " + targetBlock.getType().name());
            Bukkit.getConsoleSender().sendMessage("Block above: " + blockAbove.getType().name());
            Bukkit.getConsoleSender().sendMessage("Block two above: " + blockTwoAbove.getType().name());
            Bukkit.getConsoleSender().sendMessage("Target biome: " + targetBiome.name());
        }

        Location targetLocation = new Location(world, finalNewX + 0.5, newY, finalNewZ + 0.5);
        if (!world.getWorldBorder().isInside(targetLocation)) {
            if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: "
                        + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ());
            }
            if (attemptNumber >= maxAttempts) {
                for (String line : LoadMessages.worldborder_error) {
                    player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                }
            }
            handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
            return;
        }

        int skyLight;
        try {
            skyLight = blockAbove.getLightFromSky();
        } catch (Exception e) {
            skyLight = 15;
        }

        boolean safeSurface = !IsBlockBanned.isBlockBanned(targetBlock.getType())
                && !IsBiomeBanned.isBiomeBanned(targetBiome)
                && blockAbove.getType() == Material.AIR
                && blockTwoAbove.getType() == Material.AIR
                && (world.getEnvironment() != World.Environment.NORMAL || skyLight > 0);

        if (safeSurface && newY != -1) {
            if (loggingEnabled) {
                ValidateConfigEntries.validateConfigEntries(config);
                Bukkit.getLogger().info("Found suitable location for player " + player.getName() + " at X:" + finalNewX + ", Y:" + newY + ", Z:" + finalNewZ);
            }

            UUID playerId = player.getUniqueId();
            TeleportRequestManager.rememberLocation(playerId, finalNewX >> 4, finalNewZ >> 4);

            Location teleportLocation = (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END)
                    ? new Location(world, finalNewX + 0.5, newY, finalNewZ + 0.5)
                    : new Location(world, finalNewX + 0.5, newY, finalNewZ + 0.5);

            int teleportY = teleportLocation.getBlockY();
            int headY = teleportY + 1;
            int belowY = teleportY - 1;
            int worldMaxY = world.getMaxHeight() - 1;
            int worldMinY = world.getMinHeight();

            if (teleportY > worldMaxY || headY > worldMaxY || belowY < worldMinY) {
                if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                    Bukkit.getConsoleSender().sendMessage("Rejected teleport location for " + player.getName() + " due to invalid height range.");
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }

            Block blockBelowPlayer = world.getBlockAt(finalNewX, belowY, finalNewZ);
            Block blockAtPlayer = world.getBlockAt(finalNewX, teleportY, finalNewZ);
            Block blockAbovePlayer = world.getBlockAt(finalNewX, headY, finalNewZ);

            if (isHazardousFluid(blockBelowPlayer.getType())
                    || isHazardousFluid(blockAtPlayer.getType())
                    || isHazardousFluid(blockAbovePlayer.getType())) {
                if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                    Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber + " failed due to hazardous fluid near player position.");
                }
                handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
                return;
            }

            if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                int chunkX = teleportLocation.getBlockX() >> 4;
                int chunkZ = teleportLocation.getBlockZ() >> 4;
                Bukkit.getLogger().info("Starting asynchronous chunk loading for blocks: " + finalNewX + "," + finalNewZ + " (Chunk: " + chunkX + ", " + chunkZ + ") in world " + world.getName());
            }

            List<CompletableFuture<Chunk>> chunkFutures = GetChunksToLoad.getChunksToLoad(teleportLocation);
            CompletableFuture<Boolean> chunkLoadFuture = GetChunksToLoad.waitForChunkLoads(chunkFutures, teleportLocation, loggingEnabled);
            context.trackFuture(chunkLoadFuture);

            chunkLoadFuture.thenAccept(timedOut -> {
                if (context.isCancelled()) {
                    return;
                }

                if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
                    if (timedOut) {
                        Bukkit.getLogger().warning("Chunk preloading timed out for player " + player.getName() + ", proceeding with teleportation");
                    } else {
                        Bukkit.getLogger().info("All necessary chunks loaded, performing teleportation for player " + player.getName());
                    }
                }

                if (!player.isOnline()) {
                    TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "player offline");
                    Variables.playerSearchStatus.put(player.getName(), false);
                    return;
                }

                TeleportWithChunkLoading.teleportWithChunkLoading(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY);
                TeleportRequestManager.completeRequest(context, loggingEnabled);
                Variables.playerSearchStatus.put(player.getName(), false);

            }).exceptionally(ex -> {
                if (loggingEnabled) {
                    Bukkit.getLogger().severe("Error during chunk loading: " + ex.getMessage());
                }
                if (player.isOnline()) {
                    TeleportWithChunkLoading.teleportWithChunkLoading(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY);
                    TeleportRequestManager.completeRequest(context, loggingEnabled);
                    Variables.playerSearchStatus.put(player.getName(), false);
                }
                return null;
            });
            return;
        }

        if (loggingEnabled && shouldLogAttempt(attemptNumber)) {
            Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + attemptNumber + " failed due to banned block, biome, or unsafe block above.");
        }
        handleFailedAttempt(player, loggingEnabled, attemptNumber, maxAttempts, retryCallback, context);
    }

    private static final class CandidateSearchTask {
        final int x;
        final int z;
        final CompletableFuture<CoordinateCandidate> future;

        CandidateSearchTask(int x, int z, CompletableFuture<CoordinateCandidate> future) {
            this.x = x;
            this.z = z;
            this.future = future;
        }
    }

    private static final class CoordinateCandidate {
        final int y;
        final Biome biome;

        CoordinateCandidate(int y, Biome biome) {
            this.y = y;
            this.biome = biome;
        }
    }
}
