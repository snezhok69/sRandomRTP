package org.sRandomRTP.DifferentMethods;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.portal.PortalTeleportCooldownManager;
import org.sRandomRTP.Services.RuntimeStateRegistry;

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
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (params.equalsIgnoreCase("rtp_count")) {
            int count = state == null ? 0 : state.getRtpCount().get();
            return String.valueOf(count);
        }
        if (player == null || state == null) {
            return null;
        }
        if (params.equalsIgnoreCase("active_search")) {
            return String.valueOf(state.isPlayerSearching(player));
        }
        if (params.equalsIgnoreCase("cooldown")) {
            Long last = state.getCooldowns().get(player.getUniqueId());
            return String.valueOf(remainingSeconds(last, Variables.configCache.defaultCooldown * 1000L));
        }
        if (params.equalsIgnoreCase("biome_cooldown")) {
            Long last = state.getBiomeCooldowns().get(player.getUniqueId());
            return String.valueOf(remainingSeconds(last, Variables.configCache.defaultCooldown * 1000L));
        }
        if (params.equalsIgnoreCase("portal_cooldown")) {
            PortalTeleportCooldownManager manager = Variables.getPortalTeleportCooldownManager();
            return String.valueOf(manager == null ? 0L : millisToSeconds(manager.getRemainingMillis(player.getUniqueId())));
        }
        if (params.equalsIgnoreCase("last_world")) {
            Location location = state.getLastRtpLocations().get(player.getUniqueId());
            return location == null || location.getWorld() == null ? "" : location.getWorld().getName();
        }
        if (params.equalsIgnoreCase("last_location")) {
            Location location = state.getLastRtpLocations().get(player.getUniqueId());
            if (location == null || location.getWorld() == null) {
                return "";
            }
            return location.getWorld().getName() + " "
                    + location.getBlockX() + " "
                    + location.getBlockY() + " "
                    + location.getBlockZ();
        }
        return null;
    }

    private long remainingSeconds(Long lastStartedAt, long cooldownMs) {
        if (lastStartedAt == null || cooldownMs <= 0L) {
            return 0L;
        }
        return millisToSeconds(Math.max(0L, cooldownMs - (System.currentTimeMillis() - lastStartedAt)));
    }

    private long millisToSeconds(long millis) {
        return millis <= 0L ? 0L : (long) Math.ceil(millis / 1000.0D);
    }
}
