package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

public class SendLoadingFeedback {
    public static void sendLoadingFeedback(Player player) {
        boolean titleEnabled = Variables.titlefile.getBoolean("teleport.title-loading.titleEnabled-loading");
        boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.title-loading.subtitleEnabled-loading");

        if (titleEnabled && (!LoadMessages.titleMessage_loading.isEmpty() || (subtitleEnabled && !LoadMessages.subtitleMessage_loading.isEmpty()))) {
            String formattedTitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', LoadMessages.titleMessage_loading));
            if (subtitleEnabled) {
                String formattedSubtitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', LoadMessages.subtitleMessage_loading));
                player.sendTitle(formattedTitle, formattedSubtitle,
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleFadeIn-loading") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleStay-loading") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleFadeOut-loading") * 20));
            } else {
                player.sendTitle(formattedTitle, null,
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleFadeIn-loading") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleStay-loading") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.title-loading.titleFadeOut-loading") * 20));
            }
        }

        for (String line : LoadMessages.loading) {
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            player.sendMessage(formattedLine);
        }
    }
}
