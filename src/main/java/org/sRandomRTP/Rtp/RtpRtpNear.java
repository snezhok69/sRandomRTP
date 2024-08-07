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
import org.sRandomRTP.GetYGet.GetProtectedRegionName;
import org.sRandomRTP.GetYGet.GetSafeYCoordinate;
import org.sRandomRTP.GetYGet.GetSafeYCoordinateInEnd;
import org.sRandomRTP.GetYGet.GetSafeYCoordinateInNether;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class RtpRtpNear {
    public static void rtpRtpNear(CommandSender sender) {
        try {
            Player player = (Player) sender;
            Variables.initialPositions.put(player, player.getLocation());
            World world = player.getWorld();
            List<String> formattedMessage = LoadMessages.loading;

            for (String line : formattedMessage) {
                player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
            }


            FileConfiguration config = Variables.getInstance().getConfig();
            boolean titleEnabled = Variables.titlefile.getBoolean("teleport.titleEnabled");
            boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.subtitleEnabled");
            boolean loggingEnabled = config.getBoolean("logs", false);
            int centerX = (int) world.getWorldBorder().getCenter().getX();
            int centerZ = (int) world.getWorldBorder().getCenter().getZ();

            final int[] tries = {0};
            WrappedTask task = Variables.getFoliaLib().getImpl().runAtLocationTimer(world.getSpawnLocation(), () -> {
                try {
                    Player targetPlayer = null;
                    if (tries[0] >= Variables.teleportfile.getInt("teleport.maxtries")) {
                        List<String> formattedMessage1 = LoadMessages.locationNotFound;
                        for (String line : formattedMessage1) {
                            player.sendMessage(TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line)));
                        }
                        if (Variables.teleportTasks.containsKey(player)) {
                            WrappedTask[] tasks = Variables.teleportTasks.get(player);
                            for (WrappedTask tasks1 : tasks) {
                                tasks1.cancel();
                            }
                            Variables.teleportTasks.remove(player);
                            Variables.playerSearchStatus.put(player.getName(), false);
                            return;
                        }
                    }

                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        List<Player> allPlayers = world.getPlayers();
                        if (allPlayers.isEmpty()) {
                            tries[0]++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed because no players are online.");
                            }
                            return;
                        }

                        targetPlayer = FindRandomPlayerNear.findRandomPlayer(player, allPlayers);
                    }

                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        int minRadius = Variables.nearfile.getInt("teleport.minRadius");
                        int maxRadius = Variables.nearfile.getInt("teleport.maxRadius");

                        int randomRadius = minRadius + new Random().nextInt(maxRadius - minRadius + 1);

                        Location targetLocation = targetPlayer.getLocation();
                        double randomAngle = Math.random() * 2 * Math.PI;
                        double randomDistance = Math.sqrt(Math.random()) * randomRadius;
                        int newX = (int) (targetLocation.getBlockX() + randomDistance * Math.cos(randomAngle));
                        int newZ = (int) (targetLocation.getBlockZ() + randomDistance * Math.sin(randomAngle));

                        Location loc = new Location(world, newX, 0, newZ);
                        PaperLib.getChunkAtAsync(loc).thenAccept(chunk -> {
                            CompletableFuture<GetSafeYCoordinate.CoordinateWithBiome> coordFuture;

                            if (world.getEnvironment() == World.Environment.NETHER) {
                                coordFuture = GetSafeYCoordinateInNether.getSafeYCoordinateInNetherAsync(world, newX, newZ)
                                        .thenApply(newY -> new GetSafeYCoordinate.CoordinateWithBiome(newY, world.getBiome(newX, newY, newZ)));
                            } else if (world.getEnvironment() == World.Environment.THE_END) {
                                coordFuture = GetSafeYCoordinateInEnd.getSafeYCoordinateInEndAsync(world, newX, newZ)
                                        .thenApply(newY -> new GetSafeYCoordinate.CoordinateWithBiome(newY, world.getBiome(newX, newY, newZ)));
                            } else {
                                coordFuture = GetSafeYCoordinate.getSafeYCoordinateWithAirCheckAsync(world, newX, newZ);
                            }

                            coordFuture.thenAccept(coordWithBiome -> {
                                if (coordWithBiome == null || coordWithBiome.y == -1) {
                                    tries[0]++;
                                    if (loggingEnabled) {
                                        Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to unsafe location.");
                                    }
                                    return;
                                }

                                int newY = coordWithBiome.y;
                                Location targetTeleportLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);

                                Biome targetBiome = coordWithBiome.biome;
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

                                int worldBorderSize = (int) (world.getWorldBorder().getSize() / 2);
                                int minX = centerX - worldBorderSize;
                                int maxX = centerX + worldBorderSize;
                                int minZ = centerZ - worldBorderSize;
                                int maxZ = centerZ + worldBorderSize;

                                if (targetTeleportLocation.getX() < minX || targetTeleportLocation.getX() > maxX ||
                                        targetTeleportLocation.getZ() < minZ || targetTeleportLocation.getZ() > maxZ) {
                                    if (loggingEnabled) {
                                        Bukkit.getConsoleSender().sendMessage("Attempted to teleport outside world border to coordinates: "
                                                + targetTeleportLocation.getBlockX() + ", " + targetTeleportLocation.getBlockY() + ", " + targetTeleportLocation.getBlockZ());
                                    }
                                    tries[0]++;
                                    if (loggingEnabled) {
                                        Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to world border constraints.");
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
                                        if (Variables.teleportTasks.containsKey(player)) {
                                            WrappedTask[] tasks = Variables.teleportTasks.get(player);
                                            for (WrappedTask tasks1 : tasks) {
                                                tasks1.cancel();
                                            }
                                            Variables.teleportTasks.remove(player);
                                        }
                                        Variables.playerSearchStatus.put(player.getName(), false);
                                        return;
                                    }
                                }

                                if (Variables.teleportfile.getBoolean("teleport.checking-in-regions")) {
                                    if (IsInProtectedRegion.isInProtectedRegion(targetTeleportLocation)) {
                                        String regionName = GetProtectedRegionName.getProtectedRegionName(targetTeleportLocation);
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

                                if (!IsBlockBanned.isBlockBanned(targetBlock.getType()) && !IsBiomeBanned.isBiomeBanned(targetBiome) &&
                                        blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                                    if (loggingEnabled) {
                                        ValidateConfigEntries.validateConfigEntries(config);
                                    }

                                    if (newY != -1) {
                                        Location teleportLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);

                                        if (world.getEnvironment() == World.Environment.NETHER || world.getEnvironment() == World.Environment.THE_END) {
                                            targetTeleportLocation.setY(newY);
                                        } else {
                                            teleportLocation.setY(newY + 2);
                                        }
                                        PaperLib.teleportAsync(player, teleportLocation).thenAccept(result -> {
                                            if (result) {
                                                List<String> formattedMessage2 = LoadMessages.teleportyes;
                                                for (String line : formattedMessage2) {
                                                    line = line.replace("%x%", String.valueOf(newX));
                                                    line = line.replace("%z%", String.valueOf(newZ));
                                                    line = line.replace("%y%", String.valueOf(newY));
                                                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                                    player.sendMessage(formattedLine);
                                                }
                                            }
                                        });
                                        //
                                        EffectGivePlayer.effectGivePlayer(player);
                                        //
                                        if (Variables.teleportTasks.containsKey(player)) {
                                            WrappedTask[] tasks = Variables.teleportTasks.get(player);
                                            for (WrappedTask tasks1 : tasks) {
                                                tasks1.cancel();
                                            }
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
                                        if (Variables.teleportTasks.containsKey(player)) {
                                            WrappedTask[] tasks = Variables.teleportTasks.get(player);
                                            for (WrappedTask tasks1 : tasks) {
                                                tasks1.cancel();
                                            }
                                            Variables.teleportTasks.remove(player);
                                        }
                                        Variables.playerSearchStatus.put(player.getName(), false);
                                    } else {
                                        tries[0]++;
                                        if (loggingEnabled) {
                                            Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries[0] + " failed due to banned block, biome, or unsafe block above.");
                                        }
                                    }
                                }
                            });
                        });
                    }
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
}