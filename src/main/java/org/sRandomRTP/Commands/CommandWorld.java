package org.sRandomRTP.Commands;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.Cooldowns.CooldownBypassBossBarWorld;
import org.sRandomRTP.Cooldowns.CooldownCommandRtp;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.GetYGet.GetPlayerItemCount;
import java.util.List;
import java.util.Map;

public class CommandWorld {

    public static void commandWorld(CommandSender sender, String worldName) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
                return;
            }

            Player player = (Player) sender;
            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);

            if (!player.hasPermission("sRandomRTP.Command.World")) {
                List<String> formattedMessage = LoadMessages.nopermissioncommand;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }
                return;
            }
            World targetWorld = Bukkit.getWorld(worldName);
            if (targetWorld == null) {
                player.sendMessage("§a[sRandomRTP]  §cWorld §4'" + worldName + "' §cdoes not exist!");
                return;
            }
            if (targetWorld.getEnvironment() == World.Environment.NETHER && Variables.teleportfile.getBoolean("teleport.achievement.nether-enabled")) {
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft("story/enter_the_nether"));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    if (progress.isDone()) {
                    } else {
                        List<String> endWorldMessage = LoadMessages.noadvancementnether;
                        for (String line : endWorldMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        return;
                    }
                }
            }
            if (targetWorld.getEnvironment() == World.Environment.THE_END && Variables.teleportfile.getBoolean("teleport.achievement.the-end-enabled")) {
                Advancement advancement = Bukkit.getAdvancement(NamespacedKey.minecraft("story/enter_the_end"));
                if (advancement != null) {
                    AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    if (progress.isDone()) {
                    } else {
                        List<String> endWorldMessage = LoadMessages.noadvancementend;
                        for (String line : endWorldMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        return;
                    }
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
            if (CooldownBypassBossBarWorld.cooldownBypassBossBarworld(player, sender, worldName)) {
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
