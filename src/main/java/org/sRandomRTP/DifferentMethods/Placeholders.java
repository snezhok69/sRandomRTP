package org.sRandomRTP.DifferentMethods;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class Placeholders extends PlaceholderExpansion {
    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getIdentifier() {
        return "sRandomRTP";
    }

    public String getAuthor() {
        return Variables.getInstance().getDescription().getAuthors().toString();
    }

    public String getVersion() {
        return Variables.getInstance().getDescription().getVersion();
    }

    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("rtp_count")) {
            int count = Variables.rtpCount.getOrDefault(1, 0);
            return String.valueOf(count);
        }
        return null;
    }
}