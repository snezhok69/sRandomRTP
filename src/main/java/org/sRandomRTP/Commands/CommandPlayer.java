package org.sRandomRTP.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarPlayer;
import org.sRandomRTP.Cooldowns.CooldownCommandRtp;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.GetPlayerItemCount;
import java.util.List;
import java.util.Map;

public class CommandPlayer {

    public static void commandplayer(CommandSender sender, Player targetPlayer, World targetWorld) {
        try {
            if (targetPlayer.equals(sender)) {
                List<String> formattedMessage1 = LoadMessages.error_teleport_yourself;
                for (String line : formattedMessage1) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                    return;
                }
            }
            Player player = (sender instanceof Player) ? (Player) sender : null;
            World world = targetWorld != null ? targetWorld : targetPlayer.getWorld();
            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);

            if (player != null && !player.hasPermission("sRandomRTP.Command.Player")) {
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
                    sender.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable logs in the configuration (logs: true) and try teleportation again.");
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
                    sender.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, enable logs in the configuration (logs: true) and try teleportation again.");
                    return;
                }

                int teleportCost = Variables.economyfile.getInt("teleport.Money.money");
                if (player != null && !Variables.econ.has(player, teleportCost)) {
                    List<String> formattedMessage = LoadMessages.insufficient_funds;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%money%", String.valueOf(teleportCost))));
                        player.sendMessage(formattedLine);
                    }
                    return;
                }
                if (player != null && !Variables.econ.withdrawPlayer(player, teleportCost).transactionSuccess()) {
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
                if (targetPlayer.getFoodLevel() < requiredHunger) {
                    List<String> formattedMessage = LoadMessages.insufficient_hunger;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%hunger%", String.valueOf(requiredHunger))));
                        targetPlayer.sendMessage(formattedLine);
                    }
                    return;
                }
            }

            if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
                int requiredLevel = Variables.economyfile.getInt("teleport.Levels.level");
                if (targetPlayer.getLevel() < requiredLevel) {
                    List<String> formattedMessage = LoadMessages.insufficient_levels;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%level%", String.valueOf(requiredLevel))));
                        targetPlayer.sendMessage(formattedLine);
                    }
                    return;
                }
            }

            if (Variables.economyfile.getBoolean("teleport.Health.enabled")) {
                double requiredHealth = Variables.economyfile.getDouble("teleport.Health.health");
                if (targetPlayer.getHealth() < requiredHealth) {
                    List<String> formattedMessage = LoadMessages.insufficient_health;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line.replace("%health%", String.valueOf(requiredHealth))));
                        targetPlayer.sendMessage(formattedLine);
                    }
                    return;
                }
            }

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
                    int playerItemCount = GetPlayerItemCount.getPlayerItemCount(targetPlayer, entry.getKey());
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
                        targetPlayer.sendMessage(formattedLine);
                    }
                    return;
                }
            }

            if (PlayerSearchStatus.playerSearchStatus(targetPlayer, sender)) {
                return;
            }

            if (CooldownCommandRtp.cooldownCommandRtp(targetPlayer, sender)) {
                return;
            }

            if (CooldownBypassBossBarPlayer.cooldownBypassBossBarplayer(sender, targetPlayer, world)) {
                return;
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}
