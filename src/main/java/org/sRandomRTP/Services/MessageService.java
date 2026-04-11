package org.sRandomRTP.Services;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;

import java.util.List;

public class MessageService {

    public void send(CommandSender sender, List<String> messages) {
        send(sender, messages, new String[0]);
    }

    public void send(CommandSender sender, List<String> messages, String... replacements) {
        if (sender == null || messages == null) {
            return;
        }
        for (String line : messages) {
            sender.sendMessage(format(line, replacements));
        }
    }

    public String format(String line, String... replacements) {
        if (line == null) {
            return "";
        }
        String formatted = line;
        if (replacements != null) {
            for (int i = 0; i + 1 < replacements.length; i += 2) {
                formatted = formatted.replace(replacements[i], replacements[i + 1]);
            }
        }
        return TranslateRGBColors.translateRGBColors(
                ChatColor.translateAlternateColorCodes('&', formatted)
        );
    }
}
