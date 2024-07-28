package org.sRandomRTP.Cooldowns;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CooldownCommandRtp {
    public static boolean cooldownCommandRtp(Player player, CommandSender sender) {
        try {
            FileConfiguration config = Variables.getInstance().getConfig();
            boolean loggingEnabled = config.getBoolean("logs", false);
            if (player.hasPermission("sRandomRTP.Cooldown.bypass")) {
                Variables.cooldowns.remove(player.getName());
            }

            if (Variables.teleportfile.getBoolean("teleport.Cooldowns.enabled")) {
                int cooldown = Variables.teleportfile.getInt("teleport.Cooldowns.cooldown");

                if (loggingEnabled) {
                    Bukkit.getConsoleSender().sendMessage("Default cooldown from config: " + cooldown);
                }

                // Get the custom cooldown value based on permissions
                int customCooldown = getCustomCooldown(player, cooldown);

                if (loggingEnabled) {
                    Bukkit.getConsoleSender().sendMessage("Custom cooldown after checking permissions: " + customCooldown);
                }

                // Use the custom cooldown value
                cooldown = customCooldown;

                if (Variables.cooldowns.containsKey(player.getName())) {
                    long timeElapsed = System.currentTimeMillis() - Variables.cooldowns.get(player.getName());
                    if (timeElapsed < cooldown * 1000L) {
                        long timeLeft = cooldown * 1000L - timeElapsed;
                        List<String> formattedMessage = LoadMessages.messagescooldownMessage;
                        for (String line : formattedMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            formattedLine = formattedLine.replace("%time%", String.valueOf(timeLeft / 1000));
                            sender.sendMessage(formattedLine);
                        }
                        return true;
                    }
                }
            }

            if (!player.hasPermission("sRandomRTP.Cooldown.bypass")) {
                Variables.cooldowns.put(player.getName(), System.currentTimeMillis());
            }
            return false;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }

    private static int getCustomCooldown(Player player, int defaultCooldown) {
        FileConfiguration config = Variables.getInstance().getConfig();
        boolean loggingEnabled = config.getBoolean("logs", false);

        Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();

        Pattern pattern = Pattern.compile("(?i)sRandomRTP\\.Cooldown\\.(\\d+)");
        for (PermissionAttachmentInfo permInfo : permissions) {
            if (permInfo.getValue()) { // Check if the permission is explicitly set to true
                String permission = permInfo.getPermission();
                Matcher matcher = pattern.matcher(permission);
                if (matcher.matches()) {
                    int cooldown = Integer.parseInt(matcher.group(1));
                    if (loggingEnabled) {
                        Bukkit.getConsoleSender().sendMessage("Matching permission: " + permission + ", extracted cooldown: " + cooldown);
                        Bukkit.getConsoleSender().sendMessage("Applying custom cooldown: " + cooldown);
                    }
                    return cooldown;
                }
            }
        }
        if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage("No custom cooldown permissions found, using default: " + defaultCooldown);
        }
        return defaultCooldown;
    }
}
