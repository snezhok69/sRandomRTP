package org.sRandomRTP.DifferentMethods;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;

public class TranslateRGBColors {
    public static String translateRGBColors(String message) {
        try {
        Matcher match = Variables.RGB_PATTERN.matcher(message);
        while (match.find()) {
            String color = message.substring(match.start(), match.end());
            String hex = match.group(1);
            net.md_5.bungee.api.ChatColor chatColor = net.md_5.bungee.api.ChatColor.of("#" + hex);
            message = message.replace(color, chatColor + "");
            match = Variables.RGB_PATTERN.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return message;
    }
}