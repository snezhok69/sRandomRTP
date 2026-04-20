package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.ConfigCache;

public class SendLoadingFeedback {
    public static void sendLoadingFeedback(Player player) {
        // Read from the pre-parsed ConfigCache snapshot — eliminates live YAML getBoolean/getDouble
        // calls on every /rtp invocation and is consistent with the reload-safe design.
        final ConfigCache cfg = Variables.configCache;

        if (cfg.titleLoadingEnabled
                && (!LoadMessages.titleMessage_loading.isEmpty()
                    || (cfg.subtitleLoadingEnabled && !LoadMessages.subtitleMessage_loading.isEmpty()))) {
            // TranslateRGBColors.translateRGBColors() already calls ChatColor.translateAlternateColorCodes internally
            String formattedTitle = TranslateRGBColors.translateRGBColors(LoadMessages.titleMessage_loading);
            if (cfg.subtitleLoadingEnabled) {
                String formattedSubtitle = TranslateRGBColors.translateRGBColors(LoadMessages.subtitleMessage_loading);
                player.sendTitle(formattedTitle, formattedSubtitle,
                        cfg.titleFadeInLoading, cfg.titleStayLoading, cfg.titleFadeOutLoading);
            } else {
                player.sendTitle(formattedTitle, null,
                        cfg.titleFadeInLoading, cfg.titleStayLoading, cfg.titleFadeOutLoading);
            }
        }

        for (String line : LoadMessages.loading) {
            String formattedLine = TranslateRGBColors.translateRGBColors(line);
            player.sendMessage(formattedLine);
        }
    }
}
