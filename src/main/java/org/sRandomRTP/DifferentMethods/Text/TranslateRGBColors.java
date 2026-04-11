package org.sRandomRTP.DifferentMethods.Text;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateRGBColors {

    private static final Pattern RGB_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    public static String translateRGBColors(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher match = RGB_PATTERN.matcher(message);
        while (match.find()) {
            String color = message.substring(match.start(), match.end());
            String hex = match.group(1);
            net.md_5.bungee.api.ChatColor chatColor = net.md_5.bungee.api.ChatColor.of("#" + hex);
            message = message.replace(color, chatColor + "");
            match = RGB_PATTERN.matcher(message);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
