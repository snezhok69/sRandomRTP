package org.sRandomRTP.Rtp;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.lib.PaperLib;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.BlockBiomes.IsBiomeBanned;
import org.sRandomRTP.BlockBiomes.IsBlockBanned;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Events.PlayerParticles;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class RtpRtpWorld {

    public static void rtpRtpworld(CommandSender sender, Player player, World targetWorld) {
        try {
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
            List<String> formattedMessage = LoadMessages.loading;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                player.sendMessage(formattedLine);
            }

            FileConfiguration config = Variables.getInstance().getConfig();
            boolean titleEnabled = Variables.titlefile.getBoolean("teleport.titleEnabled");
            boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.subtitleEnabled");
            boolean loggingEnabled = config.getBoolean("logs", false);
            int centerX = (int) world.getWorldBorder().getCenter().getX();
            int centerZ = (int) world.getWorldBorder().getCenter().getZ();
            int radius = Variables.teleportfile.getInt("teleport.radius");
            int minRadius = Variables.teleportfile.getInt("teleport.minradius");

            final int[] tries = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runAtLocationTimer(player.getLocation(), () -> {
                try {
                    if (tries[0] >= Variables.teleportfile.getInt("teleport.maxtries")) {
                        List<String> formattedMessage1 = LoadMessages.locationNotFound;
                        for (String line : formattedMessage1) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                        if (checkProximityTaskTask != null) {
                            checkProximityTaskTask.cancel();
                            Variables.teleportTasks.remove(player);
                        }
                        Variables.playerSearchStatus.put(player.getName(), false);
                        return;
                    }

                    int newX = centerX + (int) ((Math.random() * radius * 2) - radius);
                    int newZ = centerZ + (int) ((Math.random() * radius * 2) - radius);

                    if (radius == minRadius) {
                        List<String> formattedMessage3 = LoadMessages.error_radius;
                        for (String line : formattedMessage3) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                        if (checkProximityTaskTask != null) {
                            checkProximityTaskTask.cancel();
                            Variables.teleportTasks.remove(player);
                        }
                        Variables.playerSearchStatus.put(player.getName(), false);
                        return;
                    }

                    AtomicReference<GetSafeYCoordinate1.CoordinateWithBiome> coordWithBiomeRef = new AtomicReference<>();

                    CompletableFuture<Integer> yCoordinateFuture;

                    if (world.getEnvironment() == World.Environment.NETHER) {
                        yCoordinateFuture = GetSafeYCoordinateInNether.getSafeYCoordinateInNetherAsync(world, newX, newZ);
                    } else if (world.getEnvironment() == World.Environment.THE_END) {
                        yCoordinateFuture = GetSafeYCoordinateInEnd.getSafeYCoordinateInEndAsync(world, newX, newZ);
                    } else {
                        yCoordinateFuture = GetSafeYCoordinate1.getSafeYCoordinateWithAirCheck(world, newX, newZ)
                                .thenApply(coord -> (coord != null && coord.y != -1) ? coord.y : -1);
                    }

                    yCoordinateFuture.thenAccept(newY -> {
                        if (!Variables.teleportTasks.containsValue(Variables.teleportTasks.get(player))) {
                            WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                            if (checkProximityTaskTask != null) {
                                checkProximityTaskTask.cancel();
                                Variables.teleportTasks.remove(player);
                            }
                            Variables.playerSearchStatus.put(player.getName(), false);
                            return;
                        }
                        if (newY == -1) {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                            }
                            return;
                        }


                        Biome targetBiome = world.getBiome(newX, newY, newZ);
                        coordWithBiomeRef.set(new GetSafeYCoordinate1.CoordinateWithBiome(newY, targetBiome));

                        Block targetBlock = world.getBlockAt(newX, newY - 1, newZ);
                        Block blockAbove = world.getBlockAt(newX, newY, newZ);
                        Block blockTwoAbove = world.getBlockAt(newX, newY + 1, newZ);

                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Trying teleport to: X=" + newX + ", Y=" + newY + ", Z=" + newZ);
                            Bukkit.getConsoleSender().sendMessage("Target block: " + targetBlock.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Block above: " + blockAbove.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Block two above: " + blockTwoAbove.getType().name());
                            Bukkit.getConsoleSender().sendMessage("Target biome: " + targetBiome.name());
                            Bukkit.getConsoleSender().sendMessage("Banned blocks: " + Variables.blockList.toString());
                            Bukkit.getConsoleSender().sendMessage("Banned biomes: " + Variables.teleportfile.getStringList("teleport.bannedBiomes").toString());
                        }

                        Location targetLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);
                        int worldBorderSize = (int) (world.getWorldBorder().getSize() / 2);
                        int minX = centerX - worldBorderSize;
                        int maxX = centerX + worldBorderSize;
                        int minZ = centerZ - worldBorderSize;
                        int maxZ = centerZ + worldBorderSize;

                        if (targetLocation.getX() < minX || targetLocation.getX() > maxX || targetLocation.getZ() < minZ || targetLocation.getZ() > maxZ) {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: "
                                        + targetLocation.getBlockX() + ", "
                                        + targetLocation.getBlockY() + ", "
                                        + targetLocation.getBlockZ());
                                Bukkit.getConsoleSender().sendMessage("Teleportation cancelled due to world border constraints.");
                            }
                            WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                            if (checkProximityTaskTask != null) {
                                checkProximityTaskTask.cancel();
                                Variables.teleportTasks.remove(player);
                            }
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
                                WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                                if (checkProximityTaskTask != null) {
                                    checkProximityTaskTask.cancel();
                                    Variables.teleportTasks.remove(player);
                                }
                                Variables.playerSearchStatus.put(player.getName(), false);
                                return;
                            }
                        }

                        if (Variables.teleportfile.getBoolean("teleport.checking-in-regions")) {
                            try {
                                Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
                            } catch (ClassNotFoundException e) {
                                if (loggingEnabled) {
                                    Bukkit.getConsoleSender().sendMessage("Install the WorldGuard plugin or disable checking regions in the configuration (checkinginregions: false).");
                                }
                                player.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable logs in the configuration (logs: true) and try teleportation again.");
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

                        if (!IsBlockBanned.isBlockBanned(targetBlock.getType())
                                && !IsBiomeBanned.isBiomeBanned(targetBiome)
                                && blockAbove.getType() == Material.AIR
                                && blockTwoAbove.getType() == Material.AIR) {
                            if (loggingEnabled) {
                                ValidateConfigEntries.validateConfigEntries(config);
                            }

                            if (newY != -1) {
                                Location teleportLocation;
                                if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
                                    teleportLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);
                                } else {
                                    teleportLocation = new Location(world, newX + 0.5, newY + 2, newZ + 0.5);
                                }

                                if (loggingEnabled) {
                                    int chunkX = teleportLocation.getBlockX() >> 4;
                                    int chunkZ = teleportLocation.getBlockZ() >> 4;
                                    Bukkit.getLogger().info("Starting asynchronous chunk loading for blocks: "
                                            + newX + ", " + newZ + " (Chunk: " + chunkX + ", " + chunkZ + ") in world " + world.getName());
                                }

                                PaperLib.getChunkAtAsync(teleportLocation).thenAccept(chunk -> {
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().info("Chunk is loaded: " + chunk.getX() + ", " + chunk.getZ());
                                    }
                                    Variables.getFoliaLib().getImpl().runLater(() -> {
                                        if (loggingEnabled) {
                                            Bukkit.getLogger().info("Begin teleporting the player " + player.getName());
                                        }

                                        PaperLib.teleportAsync(player, teleportLocation).thenAccept(success -> {
                                            if (success) {
                                                if (loggingEnabled) {
                                                    Bukkit.getLogger().info("Player " + player.getName() + " successfully teleported to "
                                                            + teleportLocation.getX() + ", "
                                                            + teleportLocation.getY() + ", "
                                                            + teleportLocation.getZ());
                                                }
                                                List<String> formattedMessage2 = LoadMessages.teleportyes;
                                                for (String line : formattedMessage2) {
                                                    line = line.replace("%x%", String.valueOf(newX));
                                                    line = line.replace("%z%", String.valueOf(newZ));
                                                    line = line.replace("%y%", String.valueOf(newY));
                                                    String formattedLine = TranslateRGBColors.translateRGBColors(
                                                            ChatColor.translateAlternateColorCodes('&', line)
                                                    );
                                                    player.sendMessage(formattedLine);

                                                    if (!Variables.teleportTasks.containsValue(Variables.teleportTasks.get(player))) {
                                                        return;
                                                    }
                                                    WrappedTask checkProximityTaskTask12 = Variables.teleportTasks.get(player);
                                                    if (checkProximityTaskTask12 != null) {
                                                        checkProximityTaskTask12.cancel();
                                                        Variables.teleportTasks.remove(player);
                                                    }
                                                    Variables.playerSearchStatus.put(player.getName(), false);
                                                }
                                            } else {
                                                if (loggingEnabled) {
                                                    Bukkit.getLogger().warning("Failed to teleport player " + player.getName());
                                                }
                                            }
                                        });
                                    }, 1L);
                                }).exceptionally(ex -> {
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().severe("Chunk loading error: " + ex.getMessage());
                                    }
                                    ex.printStackTrace();
                                    if (loggingEnabled) {
                                        player.sendMessage("Error loading the location. Try again.");
                                    }
                                    return null;
                                });
                            }
                            EffectGivePlayer.effectGivePlayer(player);
                            //
                            WrappedTask checkProximityTaskTask = Variables.teleportTasks.get(player);
                            if (checkProximityTaskTask != null) {
                                checkProximityTaskTask.cancel();
                                Variables.teleportTasks.remove(player);
                            }
                            Variables.playerSearchStatus.put(player.getName(), false);
                            if (titleEnabled && (!LoadMessages.titleMessage.isEmpty() || (subtitleEnabled && !LoadMessages.subtitleMessage.isEmpty()))) {
                                String formattedTitle = LoadMessages.titleMessage.replace("%x%", String.valueOf(newX)).replace("%z%", String.valueOf(newZ)).replace("%y%", String.valueOf(newY));
                                formattedTitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedTitle));
                                if (subtitleEnabled) {
                                    String formattedSubtitle = LoadMessages.subtitleMessage.replace("%x%", String.valueOf(newX)).replace("%z%", String.valueOf(newZ)).replace("%y%", String.valueOf(newY));
                                    formattedSubtitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedSubtitle));
                                    player.sendTitle(formattedTitle, formattedSubtitle,
                                            Variables.titlefile.getInt("teleport.titleFadeIn") * 20,
                                            Variables.titlefile.getInt("teleport.titleStay") * 20,
                                            Variables.titlefile.getInt("teleport.titleFadeOut") * 20);
                                } else {
                                    player.sendTitle(formattedTitle, null,
                                            Variables.titlefile.getInt("teleport.titleFadeIn") * 20,
                                            Variables.titlefile.getInt("teleport.titleStay") * 20,
                                            Variables.titlefile.getInt("teleport.titleFadeOut") * 20);
                                }
                            }
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                            Variables.getFoliaLib().getImpl().runLater(() -> CommandRun.commandrun(player), 0);
                            //
                            if (Variables.effectfile.getBoolean("teleport.Freeze.enabled")) {
                                String version = Bukkit.getServer().getVersion();
                                int majorVersion = Integer.parseInt(version.split("\\.")[1]);

                                if (majorVersion >= 17) {
                                    int freezeTimeInSeconds = Variables.effectfile.getInt("teleport.Freeze.time");
                                    int freezeTicks = freezeTimeInSeconds * 40;
                                    player.setFreezeTicks(freezeTicks);
                                } else {
                                    Bukkit.getConsoleSender().sendMessage("The freeze teleportation feature does not work on versions below 1.17.");
                                }
                            }
                            //
                            if (Variables.soundfile.getBoolean("teleport.completed-teleport-sound.enabled")) {
                                String soundName = Variables.soundfile.getString("teleport.completed-teleport-sound.sound");
                                float volume = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.volume");
                                float pitch = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.pitch");

                                try {
                                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                                    player.getPlayer().playSound(player.getPlayer().getLocation(), sound, volume, pitch);
                                } catch (IllegalArgumentException e) {
                                    Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + soundName);
                                }
                            }
                            if (Variables.economyfile.getBoolean("teleport.Hunger.enabled")) {
                                int newFoodLevel = player.getFoodLevel() - Variables.economyfile.getInt("teleport.Hunger.hunger");
                                player.setFoodLevel(Math.max(newFoodLevel, 0));
                            }
                            if (Variables.economyfile.getBoolean("teleport.Health.enabled")) {
                                double healthToDeduct = Variables.economyfile.getDouble("teleport.Health.health");
                                double newHealth = player.getHealth() - healthToDeduct;
                                player.setHealth(Math.max(newHealth, 0.0));
                            }
                            if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
                                int newLevel = player.getLevel() - Variables.economyfile.getInt("teleport.Levels.level");
                                player.setLevel(Math.max(newLevel, 0));
                            }
                            //
                            if (Variables.economyfile.getBoolean("teleport.Items.enabled")) {
                                for (Map.Entry<Material, Integer> entry : Variables.itemMap.entrySet()) {
                                    RemovePlayerItems.removePlayerItems(player, entry.getKey(), entry.getValue());
                                }
                            }
                            //
                            if (Variables.particlesfile.getBoolean("teleport.particles.enabled")) {
                                PlayerParticles.playerParticles(player);
                            }
                            WrappedTask checkProximityTaskTask1 = Variables.teleportTasks.get(player);
                            if (checkProximityTaskTask1 != null) {
                                checkProximityTaskTask1.cancel();
                                Variables.teleportTasks.remove(player);
                            }
                            Variables.playerSearchStatus.put(player.getName(), false);
                        } else {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to banned block, biome, or unsafe block above.");
                            }
                        }
                    });
                } catch (Throwable e) {
                    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                    String callingClassName = stackTrace[2].getClassName();
                    LoggerUtility.loggerUtility(callingClassName, e);
                }
            }, 0L, 1L);
            Variables.teleportTasks.put(player, task);
            Variables.playerSearchStatus.put(player.getName(), true);
        } catch (
                Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}
