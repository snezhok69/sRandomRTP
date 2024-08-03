package org.sRandomRTP.Rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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
import org.sRandomRTP.GetYGet.GetSafeYCoordinate;
import org.sRandomRTP.GetYGet.GetSafeYCoordinateInEnd;
import org.sRandomRTP.GetYGet.GetSafeYCoordinateInNether;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RtpRtpBase {
    public static void rtpRtpbase(CommandSender sender) {
        try {
            Player player = (Player) sender;
            Variables.initialPositions.put(player, player.getLocation());
            World world = player.getWorld();
            List<String> formattedMessage = LoadMessages.loading;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                player.sendMessage(formattedLine);
            }

            FileConfiguration config = Variables.getInstance().getConfig();
            boolean titleEnabled = Variables.titlefile.getBoolean("teleport.titleEnabled");
            boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.subtitleEnabled");
            boolean loggingEnabled = config.getBoolean("logs", false);

            final int[] tries = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runAtLocationTimer(world.getSpawnLocation(), () -> {
                try {
                    if (tries[0] >= Variables.teleportfile.getInt("teleport.maxtries")) {
                        List<String> formattedMessage2 = LoadMessages.locationNotFound;
                        for (String line : formattedMessage2) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        WrappedTask[] tasks = Variables.teleportTasks.get(player);
                        for (WrappedTask tasks1 : tasks) {
                            tasks1.cancel();
                        }
                        Variables.teleportTasks.remove(player);
                        Variables.playerSearchStatus.put(player.getName(), false);
                        return;
                    }

                    RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                    List<ProtectedRegion> regions = new ArrayList<>(regionManager.getRegions().values());

                    // Исключаем регион __global__
                    regions.removeIf(region -> region.getId().equalsIgnoreCase("__global__"));

                    if (regions.isEmpty()) {
                        player.sendMessage("No suitable regions found for teleportation.");
                        return;
                    }
                    ProtectedRegion randomRegion = regions.get(new Random().nextInt(regions.size()));
                    BlockVector3 min = randomRegion.getMinimumPoint();
                    BlockVector3 max = randomRegion.getMaximumPoint();
                    int regionradius = Variables.teleportfile.getInt("teleport.regionradius");
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
                        tries[0]++;
                        if (loggingEnabled) {
                            Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                        }
                        return;
                    }

                    CompletableFuture<Integer> futureY;
                    if (world.getEnvironment() == World.Environment.NETHER) {
                        futureY = GetSafeYCoordinateInNether.getSafeYCoordinateInNetherAsync(world, newX, newZ);
                    } else if (world.getEnvironment() == World.Environment.THE_END) {
                        futureY = GetSafeYCoordinateInEnd.getSafeYCoordinateInEndAsync(world, newX, newZ);
                    } else {
                        futureY = GetSafeYCoordinate.getSafeYCoordinateFromBottomAsync(world, newX, newZ);
                    }

                    futureY.thenAccept(newY -> {
                        if (newY == -1) {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                            }
                            return;
                        }

                        SafeYCoordinateCallback callback = resultY -> {
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Processed Y coordinate: " + resultY);
                            }
                        };

                        if (newY == -1) {
                            GetSafeYCoordinate.getSafeYCoordinateWithAirCheckAsync(world, newX, newZ)
                                    .thenAccept(coordWithBiome -> {
                                        if (coordWithBiome != null) {
                                            callback.onResult(coordWithBiome.y);
                                        } else {
                                            callback.onResult(-1);
                                        }
                                    })
                                    .exceptionally(ex -> {
                                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                                        String callingClassName = stackTrace[2].getClassName();
                                        LoggerUtility.loggerUtility(callingClassName, ex);
                                        callback.onResult(-1);
                                        return null;
                                    });
                        } else {
                            callback.onResult(newY);
                        }

                        if (newY == -1) {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                            }
                            return;
                        }

                        Biome targetBiome = world.getBiome(newX, newY, newZ);
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
                        //
                        WrappedTask[] tasks = Variables.teleportTasks.get(player);
                        for (WrappedTask tasks1 : tasks) {
                            tasks1.cancel();
                        }
                        Variables.teleportTasks.remove(player);
                        Variables.playerSearchStatus.put(player.getName(), false);
                        //
                        if (!IsBlockBanned.isBlockBanned(targetBlock.getType())
                                && !IsBiomeBanned.isBiomeBanned(targetBiome)
                                && blockAbove.getType() == Material.AIR
                                && blockTwoAbove.getType() == Material.AIR) {
                            if (loggingEnabled) {
                                ValidateConfigEntries.validateConfigEntries(config);
                            }
                            if (newY != -1) {
                                Location teleportLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);

                                if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
                                    teleportLocation.setY(newY);
                                } else {
                                    teleportLocation.setY(newY + 2);
                                }
                                Integer finalNewY = newY;
                                PaperLib.getChunkAtAsync(teleportLocation).thenAccept(chunk -> {
                                    PaperLib.teleportAsync(player, teleportLocation).thenAccept(result -> {
                                        if (result) {
                                            List<String> formattedMessage2 = LoadMessages.teleportyes;
                                            for (String line : formattedMessage2) {
                                                line = line.replace("%x%", String.valueOf(newX));
                                                line = line.replace("%z%", String.valueOf(newZ));
                                                line = line.replace("%y%", String.valueOf(finalNewY));
                                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                                player.sendMessage(formattedLine);
                                            }
                                        }
                                    });
                                });
                            }
                            //
                            EffectGivePlayer.effectGivePlayer(player);
                            //
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
                            CommandRun.commandrun(player);
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
            }, 0, 1);
            Variables.teleportTasks.put(player, new WrappedTask[]{task});
            Variables.playerSearchStatus.put(player.getName(), true);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }

    public interface SafeYCoordinateCallback {
        void onResult(int newY);
    }
}