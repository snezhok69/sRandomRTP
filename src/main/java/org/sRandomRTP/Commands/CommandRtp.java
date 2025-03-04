package org.sRandomRTP.Commands;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBar;
import org.sRandomRTP.Cooldowns.CooldownCommandRtp;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.GetPlayerItemCount;
import org.sRandomRTP.Rtp.RtpRtpPlayer;

import java.util.List;
import java.util.Map;

public class CommandRtp {
    public static void commandRtp(CommandSender sender) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
                return;
            }
            Player player = (Player) sender;
            World world = player.getWorld();
            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);
            if (!player.hasPermission("sRandomRTP.Command.Rtp")) {
                List<String> formattedMessage = LoadMessages.nopermissioncommand;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
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
                    return;
                }
            }
            if (Variables.economyfile.getBoolean("teleport.Money.enabled")) {
                try {
                    Class.forName("net.milkbowl.vault.economy.Economy");
                } catch (ClassNotFoundException e) {
                    if (loggingEnabled) {
                        Bukkit.getConsoleSender().sendMessage("Install the Vault plugin to make the economy function work. Or disable the economy function (Money: enabled: false)");
                    }
                    player.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable logs in the configuration (logs: true) and try teleportation again.");
                    return;
                }
            }
            if (Variables.economyfile.getBoolean("teleport.Money.enabled")) {
                int teleportCost = Variables.economyfile.getInt("teleport.Money.money");
                if (!Variables.econ.has(player, teleportCost)) {
                    List<String> formattedMessage = LoadMessages.insufficient_funds;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%money%", String.valueOf(teleportCost))));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
                if (!Variables.econ.withdrawPlayer(player, teleportCost).transactionSuccess()) {
                    List<String> formattedMessage = LoadMessages.error_withdrawing;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
            }

            if (Variables.economyfile.getBoolean("teleport.Hunger.enabled")) {
                int requiredHunger = Variables.economyfile.getInt("teleport.Hunger.hunger");
                if (player.getFoodLevel() < requiredHunger) {
                    List<String> formattedMessage = LoadMessages.insufficient_hunger;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%hunger%", String.valueOf(requiredHunger))));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
            }

            if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
                int requiredLevel = Variables.economyfile.getInt("teleport.Levels.level");
                if (player.getLevel() < requiredLevel) {
                    List<String> formattedMessage = LoadMessages.insufficient_levels;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%level%", String.valueOf(requiredLevel))));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
            }
            if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
                double requiredHealth = Variables.economyfile.getDouble("teleport.Levels.health");
                if (player.getHealth() < requiredHealth) {
                    List<String> formattedMessage = LoadMessages.insufficient_health;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%health%", String.valueOf(requiredHealth))));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
            }
            if (Variables.teleportfile.getBoolean("teleport.bannedworld.enabled")) {
                List<String> bannedWorlds = Variables.teleportfile.getStringList("teleport.bannedworld.worlds");
                if (bannedWorlds.contains(world.getName())) {
                    if (Variables.teleportfile.getBoolean("teleport.bannedworld.redirect.enabled")) {
                        String redirectWorldName = Variables.teleportfile.getString("teleport.bannedworld.redirect.world");
                        World redirectWorld = Bukkit.getWorld(redirectWorldName);
                        if (redirectWorld != null) {
                            if (bannedWorlds.contains(redirectWorldName)) {
                                List<String> formattedMessage = LoadMessages.banned_world;
                                for (String line : formattedMessage) {
                                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%world%", world.getName())));
                                    player.sendMessage(formattedLine);
                                }
                                return;
                            }
                            String originalWorldName = world.getName();
                            world = redirectWorld;
                            List<String> formattedMessage = LoadMessages.redirect_world;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&',
                                        line.replace("%from_world%", originalWorldName)
                                                .replace("%to_world%", redirectWorld.getName())));
                                player.sendMessage(formattedLine);
                            }
                        } else {
                            List<String> formattedMessage = LoadMessages.banned_world;
                            for (String line : formattedMessage) {
                                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%world%", world.getName())));
                                player.sendMessage(formattedLine);
                            }
                            return;
                        }
                    } else {
                        List<String> formattedMessage = LoadMessages.banned_world;
                        for (String line : formattedMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%world%", world.getName())));
                            player.sendMessage(formattedLine);
                        }
                        return;
                    }
                }
            }
            //
            if (Variables.economyfile.getBoolean("teleport.Items.enabled")) {
                List<String> requiredItems = Variables.economyfile.getStringList("teleport.Items.requiredItems");
                for (String itemString : requiredItems) {
                    String[] parts = itemString.split(": ");
                    Material material = Material.getMaterial(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    Variables.itemMap.put(material, amount);
                }
                boolean hasAllItems = true;
                StringBuilder missingItems = new StringBuilder();
                for (Map.Entry<Material, Integer> entry : Variables.itemMap.entrySet()) {
                    int playerItemCount = GetPlayerItemCount.getPlayerItemCount(player, entry.getKey());
                    if (playerItemCount < entry.getValue()) {
                        hasAllItems = false;
                        if (missingItems.length() > 0) {
                            missingItems.append(", ");
                        }
                        missingItems.append(entry.getKey().name()).append(": ").append(entry.getValue() - playerItemCount);
                    }
                }
                if (!hasAllItems) {
                    String missingItemsMessage = missingItems.toString();
                    List<String> formattedMessage = LoadMessages.insufficient_items;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%items%", missingItemsMessage)));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
            }
            //
            if (PlayerSearchStatus.playerSearchStatus(player, sender)) {
                return;
            }
            //
            if (CooldownCommandRtp.cooldownCommandRtp(player, sender)) {
                return;
            }
            //
            if (CooldownBypassBossBar.cooldownBypassBossBar(player, sender, world)) {
                return;
            }
            return;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}