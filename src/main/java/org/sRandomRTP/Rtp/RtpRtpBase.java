package org.sRandomRTP.Rtp;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.BlockBiomes.IsBiomeBanned;
import org.sRandomRTP.BlockBiomes.IsBlockBanned;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Events.PlayerParticles;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.GetSafeYCoordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
            int radius = Variables.teleportfile.getInt("teleport.radius");

            BukkitTask task = new BukkitRunnable() {
                int tries = 0;

                @Override
                public void run() {
                    try {
                        Player targetPlayer = null;

                        if (tries >= Variables.teleportfile.getInt("teleport.maxtries")) {
                            List<String> formattedMessage = LoadMessages.locationNotFound;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                player.sendMessage(formattedLine);
                            }
                            if (Variables.teleportTasks.containsKey(player)) {
                                BukkitTask[] tasks = Variables.teleportTasks.get(player);
                                for (BukkitTask tasks1 : tasks) {
                                    tasks1.cancel();
                                }
                                Variables.teleportTasks.remove(player);
                            }
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
                            newX = max.getBlockX() + 10 + random.nextInt(regionradius);
                        } else {
                            newX = min.getBlockX() - 10 - random.nextInt(regionradius);
                        }
                        if (offsetZPositive) {
                            newZ = max.getBlockZ() + 10 + random.nextInt(regionradius);
                        } else {
                            newZ = min.getBlockZ() - 10 - random.nextInt(regionradius);
                        }
                        if (newX == 0 && newZ == 0) {
                            tries++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries + " failed due to unsafe location.");
                            }
                            return;
                        }

                        // Получение безопасной координаты Y снизу вверх (под регионом)
                        int newY = GetSafeYCoordinate.getSafeYCoordinateFromBottom(world, newX, newZ);

                        // Если безопасная координата Y снизу вверх не найдена, пытаемся получить координату сверху вниз
                        if (newY == -1) {
                            GetSafeYCoordinate.CoordinateWithBiome coordWithBiome = GetSafeYCoordinate.getSafeYCoordinateWithAirCheck(world, newX, newZ);
                            if (coordWithBiome != null) {
                                newY = coordWithBiome.y;
                            }
                        }

                        // Если безопасное место все еще не найдено
                        if (newY == -1) {
                            tries++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries + " failed due to unsafe location.");
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

                        Location targetLocation = new Location(world, newX + 0.5, newY, newZ + 0.5);

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

                                player.teleportAsync(teleportLocation);
                                List<String> formattedMessage = LoadMessages.teleportyes;
                                for (String line : formattedMessage) {
                                    line = line.replace("%x%", String.valueOf(newX));
                                    line = line.replace("%z%", String.valueOf(newZ));
                                    line = line.replace("%y%", String.valueOf(newY));
                                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                                    player.sendMessage(formattedLine);
                                }
                            }
                            //
                            if (Variables.effectfile.getBoolean("teleport.Enabled")) {
                                List<String> effectGive = Variables.effectfile.getStringList("teleport.Effect");
                                int duration = Variables.effectfile.getInt("teleport.effectDuration") * 20;
                                int amplifier = Variables.effectfile.getInt("teleport.effectAmplifier");
                                for (String effect : effectGive) {
                                    try {
                                        int effectId = Integer.parseInt(effect);
                                        PotionEffectType effectType = PotionEffectType.getById(effectId);
                                        if (effectType == null) {
                                            if (loggingEnabled) {
                                                Bukkit.getConsoleSender().sendMessage("Invalid effect ID: " + effectId);
                                            }
                                            continue;
                                        }
                                        player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, false), true);
                                        if (loggingEnabled) {
                                            Bukkit.getConsoleSender().sendMessage("Applied effect: " + effectType.getName() + " with duration: " + duration + " and amplifier: " + amplifier);
                                        }
                                    } catch (NumberFormatException e) {
                                        if (loggingEnabled) {
                                            Bukkit.getConsoleSender().sendMessage("Invalid effect format: " + effect);
                                        }
                                    } catch (Exception e) {
                                        if (loggingEnabled) {
                                            Bukkit.getConsoleSender().sendMessage("Error applying effect: " + effect + " - " + e.getMessage());
                                        }
                                    }
                                }
                            }
                            //
                            if (Variables.teleportTasks.containsKey(player)) {
                                BukkitTask[] tasks = Variables.teleportTasks.get(player);
                                for (BukkitTask tasks1 : tasks) {
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
                                BukkitTask[] tasks = Variables.teleportTasks.get(player);
                                for (BukkitTask tasks1 : tasks) {
                                    tasks1.cancel();
                                }
                                Variables.teleportTasks.remove(player);
                            }
                            Variables.playerSearchStatus.put(player.getName(), false);
                        } else {
                            tries++;
                            if (loggingEnabled) {
                                Bukkit.getConsoleSender().sendMessage("Teleportation attempt #" + tries + " failed due to banned block, biome, or unsafe block above.");
                            }
                        }
                    } catch (Throwable e) {
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        String callingClassName = stackTrace[2].getClassName();
                        LoggerUtility.loggerUtility(callingClassName, e);
                    }
                }
            }.runTaskTimer(Variables.getInstance(), 0, 1);
            Variables.teleportTasks.put(player, new BukkitTask[]{task});
            Variables.playerSearchStatus.put(player.getName(), true);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}