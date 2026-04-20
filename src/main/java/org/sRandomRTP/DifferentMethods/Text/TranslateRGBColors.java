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
        // Single-pass O(N) replacement using appendReplacement/appendTail.
        // The previous loop-with-replace approach was O(N²) because it rebuilt
        // a new Matcher on the partially-replaced string after every substitution.
        StringBuffer sb = new StringBuffer();
        Matcher match = RGB_PATTERN.matcher(message);
        while (match.find()) {
            net.md_5.bungee.api.ChatColor chatColor =
                    net.md_5.bungee.api.ChatColor.of("#" + match.group(1));
            match.appendReplacement(sb, Matcher.quoteReplacement(chatColor.toString()));
        }
        match.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }
}
