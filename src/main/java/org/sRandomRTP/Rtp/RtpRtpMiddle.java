package org.sRandomRTP.Rtp;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.BlockBiomes.IsBiomeBanned;
import org.sRandomRTP.BlockBiomes.IsBlockBanned;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.DifferentMethods.IsIn.IsInProtectedRegion;
import org.sRandomRTP.DifferentMethods.Player.EffectGivePlayer;
import org.sRandomRTP.DifferentMethods.Teleport.GenerateCoordinates;
import org.sRandomRTP.DifferentMethods.Teleport.GetChunksToLoad;
import org.sRandomRTP.DifferentMethods.Teleport.StopExistingSearchTasks;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportWithChunkLoading;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.sRandomRTP.DifferentMethods.Variables.pluginName;

public class RtpRtpMiddle {

    public static void rtpRtpmiddle(CommandSender sender, World targetWorld) {
        try {
            Player player = (Player) sender;
            if (Variables.playerSearchStatus.getOrDefault(player.getName(), false)) {
                boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
                if (loggingEnabled) {
                    Bukkit.getLogger().info("Player " + player.getName() + " requested teleportation while already in teleportation process. Cancelling previous teleportation.");
                }
                StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);
            }

            Variables.initialPositions.put(player, player.getLocation());
            final World world;

            if (targetWorld != null) {
                world = targetWorld;
            } else {
                World currentWorld = player.getWorld();
                if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                    List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                    if (bannedWorlds.contains(currentWorld.getName())) {
                        if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                            String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                            World redirectWorld = Bukkit.getWorld(redirectWorldName);
                            if (redirectWorld != null) {
                                world = redirectWorld;
                            } else {
                                world = currentWorld;
                            }
                        } else {
                            world = currentWorld;
                        }
                    } else {
                        world = currentWorld;
                    }
                } else {
                    world = currentWorld;
                }
            }

            boolean titleEnabled = Variables.titlefile.getBoolean("teleport.title-loading.titleEnabled-loading");
            boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.title-loading.subtitleEnabled-loading");

            if (titleEnabled && (!LoadMessages.titleMessage_loading.isEmpty() || (subtitleEnabled && !LoadMessages.subtitleMessage_loading.isEmpty()))) {
                String formattedTitle = LoadMessages.titleMessage_loading;
                formattedTitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedTitle));
                if (subtitleEnabled) {
                    String formattedSubtitle = LoadMessages.subtitleMessage_loading;
                    formattedSubtitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedSubtitle));
                    player.sendTitle(formattedTitle, formattedSubtitle,
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleFadeIn-loading") * 20),
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleStay-loading") * 20),
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleFadeOut-loading") * 20));
                } else {
                    player.sendTitle(formattedTitle, null,
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleFadeIn-loading") * 20),
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleStay-loading") * 20),
                            (int)(Variables.titlefile.getDouble("teleport.title-loading.titleFadeOut-loading") * 20));
                }
            }

            List<String> formattedMessage = LoadMessages.loading;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                player.sendMessage(formattedLine);
            }

            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);
            int centerX = (int) world.getWorldBorder().getCenter().getX();
            int centerZ = (int) world.getWorldBorder().getCenter().getZ();
            int radius = Variables.teleportfile.getInt("teleport-middle.radius-middle");
            int minRadius = Variables.teleportfile.getInt("teleport-middle.minradius-middle");

            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Debug: radius = " + radius + ", minRadius = " + minRadius);
            }

            if (minRadius >= radius) {
                player.sendMessage(pluginName + " §8- §cThe minimum radius cannot be greater than or equal to the maximum radius.");
                return;
            }

            if (radius - minRadius < 50) {
                player.sendMessage(pluginName + " §8- §cThe difference between the minimum and maximum radius must be at least 50 blocks.");
                return;
            }

            final int[] tries = {0};
            AtomicReference<WrappedTask> taskRef = new AtomicReference<>();

            StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);

            Variables.playerSearchStatus.put(player.getName(), true);
            if (loggingEnabled) {
                Bukkit.getLogger().info("Setting playerSearchStatus to true for player " + player.getName() + " before starting coordinate search");
            }

            WrappedTask task = Variables.getFoliaLib().getImpl().runAtLocationTimer(player.getLocation(), () -> {
                try {
                    if (!Variables.playerSearchStatus.getOrDefault(player.getName(), false)) {
                        if (loggingEnabled) {
                            Variables.getInstance().getLogger().info("Finding coordinates for a player " + player.getName() + " canceled because the player has already been teleported or teleportation is in progress.");
                        }
                        return;
                    }

                    if (tries[0] >= Variables.teleportfile.getInt("teleport.maxtries")) {
                        List<String> formattedMessage1 = LoadMessages.locationNotFound;
                        for (String line : formattedMessage1) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);
                        Variables.playerSearchStatus.put(player.getName(), false);
                        return;
                    }

                    if (radius == minRadius) {
                        List<String> formattedMessage3 = LoadMessages.error_radius;
                        for (String line : formattedMessage3) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);
                        Variables.playerSearchStatus.put(player.getName(), false);
                        return;
                    }

                    String generationMethod = Variables.teleportfile.getString("teleport.coordinate-generation");
                    boolean useAbsoluteCoordinates = Variables.teleportfile.getBoolean("teleport.use-absolute-coordinates", false);

                    if (loggingEnabled) {
                        Bukkit.getConsoleSender().sendMessage("Teleportation mode: " + generationMethod + ", Absolute coordinates: " + (useAbsoluteCoordinates ? "enabled" : "disabled") + ", Min radius: " + minRadius + ", Max radius: " + radius);
                    }

                    int[] generatedCoordinates = GenerateCoordinates.generateCoordinates(centerX, centerZ, radius, minRadius, generationMethod, useAbsoluteCoordinates);

                    int newX = generatedCoordinates[0];
                    int newZ = generatedCoordinates[1];

                    if (loggingEnabled) {
                        double distance = Math.sqrt(Math.pow(newX - centerX, 2) + Math.pow(newZ - centerZ, 2));
                        Bukkit.getConsoleSender().sendMessage("Generated coordinates: X=" + newX + ", Z=" + newZ + ", Distance from center: " + (int) distance + " blocks");
                    }

                    final int finalNewX = newX;
                    final int finalNewZ = newZ;

                    if (!Variables.playerSearchStatus.getOrDefault(player.getName(), true)) {
                        if (loggingEnabled) {
                            Bukkit.getLogger().info("Skipping Y-coordinate search for player " + player.getName() + " as suitable location has already been found");
                        }
                        return;
                    }

                    AtomicReference<GetSafeYCoordinate.CoordinateWithBiome> coordWithBiomeRef = new AtomicReference<>();

                    CompletableFuture<Integer> yCoordinateFuture;

                    if (world.getEnvironment() == World.Environment.NETHER) {
                        yCoordinateFuture = GetSafeYCoordinateInNether.getSafeYCoordinateInNetherAsync(world, finalNewX, finalNewZ);
                    } else if (world.getEnvironment() == World.Environment.THE_END) {
                        yCoordinateFuture = GetSafeYCoordinateInEnd.getSafeYCoordinateInEndAsync(world, finalNewX, finalNewZ);
                    } else {
                        yCoordinateFuture = GetSafeYCoordinate.getSafeYCoordinateWithAirCheckAsync(world, finalNewX, finalNewZ).thenApply(coord -> (coord != null && coord.y != -1) ? coord.y : -1);
                    }

                    yCoordinateFuture.thenAccept(newY -> {
                        if (!Variables.playerSearchStatus.getOrDefault(player.getName(), false)) {
                            if (loggingEnabled) {
                                Bukkit.getLogger().info("Skipping Y-coordinate processing for player " + player.getName() + " as suitable location has already been found");
                            }
                            return;
                        }

                        if (newY == -1) {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                            }
                            return;
                        }

                        Biome targetBiome = world.getBiome(finalNewX, newY, finalNewZ);
                        coordWithBiomeRef.set(new GetSafeYCoordinate.CoordinateWithBiome(newY, targetBiome));

                        Block targetBlock = world.getBlockAt(finalNewX, newY - 1, finalNewZ);
                        Block blockAbove = world.getBlockAt(finalNewX, newY, finalNewZ);
                        Block blockTwoAbove = world.getBlockAt(finalNewX, newY + 1, finalNewZ);

                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Trying teleport to: X=" + finalNewX + ", Y=" + newY + ", Z=" + finalNewZ);
                            Bukkit.getConsoleSender().sendMessage("Target block: " + targetBlock.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Block above: " + blockAbove.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Block two above: " + blockTwoAbove.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Target biome: " + targetBiome.name());
                            Bukkit.getConsoleSender().sendMessage("Banned blocks: " + Variables.blockList.toString());
                            Bukkit.getConsoleSender().sendMessage("Banned biomes: " + Variables.teleportfile.getStringList("teleport.bannedBiomes").toString());
                        }

                        Location targetLocation = new Location(world, finalNewX + 0.5, newY, finalNewZ + 0.5);
                        int worldBorderSize = (int) (world.getWorldBorder().getSize() / 2);
                        int minX = centerX - worldBorderSize;
                        int maxX = centerX + worldBorderSize;
                        int minZ = centerZ - worldBorderSize;
                        int maxZ = centerZ + worldBorderSize;

                        if (targetLocation.getX() < minX || targetLocation.getX() > maxX || targetLocation.getZ() < minZ || targetLocation.getZ() > maxZ) {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: " + targetLocation.getBlockX() + ", " + targetLocation.getBlockY() + ", " + targetLocation.getBlockZ());
                                Bukkit.getConsoleSender().sendMessage("Teleportation cancelled due to world border constraints.");
                            }
                            StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);
                            Variables.playerSearchStatus.put(player.getName(), false);
                            List<String> formattedMessage3 = LoadMessages.worldborder_error;
                            for (String line : formattedMessage3) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                player.sendMessage(formattedLine);
                            }
                            return;
                        }

                        if (Variables.teleportfile.getBoolean("teleport.checking-in-regions")) {
                            try {
                                Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                            } catch (ClassNotFoundException e) {
                                if (loggingEnabled) {
                                    Bukkit.getConsoleSender().sendMessage("Install the WorldGuard plugin or disable checking regions in the configuration (checkinginregions: false).");
                                }
                                player.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable logs in the configuration (logs: true) and try teleportation again.");
                                StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);
                                Variables.playerSearchStatus.put(player.getName(), false);
                                return;
                            }
                        }

                        if (Variables.teleportfile.getBoolean("teleport.checking-in-regions")) {
                            if (IsInProtectedRegion.isInProtectedRegion(targetLocation)) {
                                String regionName = GetProtectedRegionName.getProtectedRegionName(targetLocation);
                                if (loggingEnabled) {
                                    Bukkit.getConsoleSender().sendMessage("Attempted to teleport into protected region: " + regionName);
                                }
                                tries[0]++;
                                if (loggingEnabled) {
                                    Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to protected region.");
                                }
                                return;
                            }
                        }

                        int skyLight = 0;
                        try {
                            skyLight = blockAbove.getLightFromSky();
                        } catch (Exception e) {
                            if (loggingEnabled) {
                                Bukkit.getLogger().warning("Could not get light from sky, using default value");
                            }
                            skyLight = 15;
                        }

                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Block banned: " + IsBlockBanned.isBlockBanned(targetBlock.getType()));
                            Bukkit.getConsoleSender().sendMessage("Biome banned: " + IsBiomeBanned.isBiomeBanned(targetBiome));
                            Bukkit.getConsoleSender().sendMessage("Sky light value: " + skyLight);
                        }

                        if (!IsBlockBanned.isBlockBanned(targetBlock.getType()) && !IsBiomeBanned.isBiomeBanned(targetBiome) && blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR && (world.getEnvironment() != World.Environment.NORMAL || skyLight > 0)) {
                            if (loggingEnabled) {
                                ValidateConfigEntries.validateConfigEntries(config);
                            }
                            if (newY != -1) {
                                if (loggingEnabled) {
                                    Bukkit.getLogger().info("Found suitable location for player " + player.getName() + " at X:" + finalNewX + ", Y:" + newY + ", Z:" + finalNewZ);
                                }

                                Location teleportLocation;
                                if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
                                    teleportLocation = new Location(world, finalNewX + 0.5, newY, finalNewZ + 0.5);
                                } else {
                                    teleportLocation = new Location(world, finalNewX + 0.5, newY + 2, finalNewZ + 0.5);
                                }

                                if (loggingEnabled) {
                                    int chunkX = teleportLocation.getBlockX() >> 4;
                                    int chunkZ = teleportLocation.getBlockZ() >> 4;
                                    Bukkit.getLogger().info("Starting asynchronous chunk loading for blocks: " + finalNewX + ", " + finalNewZ + " (Chunk: " + chunkX + ", " + chunkZ + ") in world " + world.getName());
                                }

                                boolean isChunkGenerated = PaperLib.isChunkGenerated(world, teleportLocation.getBlockX() >> 4, teleportLocation.getBlockZ() >> 4);
                                if (loggingEnabled) {
                                    Bukkit.getLogger().info("Chunk " + (teleportLocation.getBlockX() >> 4) + ", " + (teleportLocation.getBlockZ() >> 4) + (isChunkGenerated ? " is already generated" : " is not generated, will be loaded asynchronously"));
                                }

                                Variables.playerSearchStatus.put(player.getName(), false);
                                if (loggingEnabled) {
                                    Bukkit.getLogger().info("Setting playerSearchStatus to false to prevent further coordinate searches");
                                    Bukkit.getLogger().info("Found suitable location at X:" + finalNewX + ", Y:" + newY + ", Z:" + finalNewZ + ", preparing for teleportation");
                                }

                                StopExistingSearchTasks.stopExistingSearchTasks(player, loggingEnabled);

                                Variables.getFoliaLib().getImpl().runAtLocation(teleportLocation, (teleportTask) -> {
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().info("Starting chunk loading for teleportation of player " + player.getName());
                                        Bukkit.getLogger().info("Player online status: " + player.isOnline());
                                        Bukkit.getLogger().info("Current playerSearchStatus: " + Variables.playerSearchStatus.getOrDefault(player.getName(), false));
                                    }

                                    List<CompletableFuture<Chunk>> chunkFutures = GetChunksToLoad.getChunksToLoad(teleportLocation);
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().info("Created chunk futures list with " + chunkFutures.size() + " chunks to load");
                                    }

                                    CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0])).thenRun(() -> {
                                        if (loggingEnabled) {
                                            Bukkit.getLogger().info("All necessary chunks loaded, performing teleportation for player " + player.getName());
                                            Bukkit.getLogger().info("Current playerSearchStatus: " + Variables.playerSearchStatus.getOrDefault(player.getName(), false));
                                            Bukkit.getLogger().info("Player online status: " + player.isOnline());
                                        }

                                        if (player == null || teleportLocation == null || teleportLocation.getWorld() == null) {
                                                if (loggingEnabled) {
                                                Bukkit.getLogger().severe("Cannot teleport: " + (player == null ? "Player is null" : teleportLocation == null ? "Location is null" : "World is null"));
                                            }
                                                        return;
                                                    }

                                        if (!player.isOnline()) {
                                                if (loggingEnabled) {
                                                Bukkit.getLogger().warning("Player " + player.getName() + " is offline, teleportation cancelled");
                                            }
                                            return;
                                        }

                                        if (loggingEnabled) {
                                            Bukkit.getLogger().info("About to call teleportWithChunkLoading for player " + player.getName() + " to location " + teleportLocation.getBlockX() + ", " + teleportLocation.getBlockY() + ", " + teleportLocation.getBlockZ());
                                        }

                                        TeleportWithChunkLoading.teleportWithChunkLoading(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY);

                                        if (loggingEnabled) {
                                            Bukkit.getLogger().info("Called teleportWithChunkLoading for player " + player.getName() + " to location " + teleportLocation);
                                        }

                                        EffectGivePlayer.effectGivePlayer(player);
                                }).exceptionally(ex -> {
                                    if (loggingEnabled) {
                                            Bukkit.getLogger().severe("Error during chunk loading: " + ex.getMessage());
                                            ex.printStackTrace();
                                            Bukkit.getLogger().info("Continuing coordinate search for player " + player.getName() + " due to chunk loading error");
                                        }

                                        if (player.isOnline()) {

                                            tries[0]++;

                                            Variables.playerSearchStatus.put(player.getName(), true);

                                    if (loggingEnabled) {
                                                Bukkit.getLogger().info("Setting playerSearchStatus to true to continue coordinate search");
                                                Bukkit.getLogger().info("Current attempt count: " + tries[0]);
                                            }

                                    }
                                    return null;
                                    });
                                });
                                return;
                            }
                        } else {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to banned block, biome, or unsafe block above.");
                            }
                        }
                    });
                    if (!Variables.playerSearchStatus.getOrDefault(player.getName(), true)) {
                        if (loggingEnabled) {
                            Bukkit.getLogger().info("Stopping coordinate search for player " + player.getName() + " as suitable location has been found");
                        }
                        return;
                    }
                } catch (Throwable e) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    String callingClassName = stackTrace[2].getClassName();
                    LoggerUtility.loggerUtility(callingClassName, e);
                }
            }, 0L, 1L);
            Variables.teleportTasks.put(player, task);
            if (loggingEnabled) {
                Bukkit.getLogger().info("Saved search task for player " + player.getName() + " in teleportTasks map");
                Bukkit.getLogger().info("Current teleportTasks size: " + Variables.teleportTasks.size());
            }

            Variables.currentSearchingPlayer = player.getName();
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}