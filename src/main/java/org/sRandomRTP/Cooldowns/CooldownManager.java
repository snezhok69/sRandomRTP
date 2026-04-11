package org.sRandomRTP.Cooldowns;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Commands.Permissions;
import org.sRandomRTP.Files.LoadMessages;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared cooldown logic used by both the standard RTP and biome RTP commands.
 * Callers pass in the specific cooldown map stored in {@link Variables}.
 */
public final class CooldownManager {

    private static final Pattern COOLDOWN_PERMISSION_PATTERN = Pattern.compile("(?i)sRandomRTP\\.Cooldown\\.(\\d+)");
    /** Cache: player UUID → [customCooldown, cachedAtMillis]. Avoids scanning all permissions on every RTP. */
    private static final Map<UUID, long[]> cooldownPermissionCache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = org.sRandomRTP.Utils.PluginConstants.COOLDOWN_PERMISSION_CACHE_TTL_MS;

    private CooldownManager() {}

    /** Invalidate cached permission lookup for a player (call on cooldown removal). */
    public static void invalidateCache(UUID playerId) {
        cooldownPermissionCache.remove(playerId);
    }

    /** Evict cache entries older than 2×TTL — call from a periodic cleanup task. */
    public static void evictExpiredCacheEntries() {
        long now = System.currentTimeMillis();
        cooldownPermissionCache.entrySet().removeIf(e -> (now - e.getValue()[1]) > CACHE_TTL_MS * 2);
    }

    /**
     * Checks (and records) a per-player cooldown.
     *
     * @param player         the teleporting player
     * @param sender         who receives the "on cooldown" message
     * @param cooldownMap    the map that stores timestamps for this command type
     * @param loggingEnabled whether debug messages should be sent to console
     * @return {@code true} if the player is still on cooldown (teleport should be blocked),
     *         {@code false} if the cooldown has expired (or cooldowns are disabled)
     */
    public static boolean checkCooldown(Player player, CommandSender sender,
                                        Map<UUID, Long> cooldownMap, boolean loggingEnabled) {
        UUID playerId = player.getUniqueId();
        if (Variables.cachedCooldownsEnabled) {
            if (player.hasPermission(Permissions.COOLDOWN_BYPASS)) {
                // Обновляем timestamp при bypass, чтобы после отзыва bypass
                // кулдаун отсчитывался с момента последнего телепорта
                cooldownMap.put(playerId, System.currentTimeMillis());
                invalidateCache(playerId);
                return false;
            }

            int cooldown = Variables.cachedDefaultCooldown;

            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Default cooldown from config: " + cooldown);
            }
            cooldown = getCustomCooldown(player, cooldown, loggingEnabled);
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Custom cooldown after checking permissions: " + cooldown);
            }

            Long lastTime = cooldownMap.get(playerId);
            if (lastTime != null) {
                long timeElapsed = System.currentTimeMillis() - lastTime;
                if (timeElapsed < cooldown * 1000L) {
                    long timeLeft = cooldown * 1000L - timeElapsed;
                    Variables.getMessageService().send(sender, LoadMessages.messagescooldownMessage,
                            "%time%", String.valueOf(timeLeft / 1000));
                    return true;
                }
            }
        }

        cooldownMap.put(playerId, System.currentTimeMillis());
        return false;
    }

    /**
     * Convenience wrapper: checks the standard RTP cooldown for a player.
     */
    public static boolean checkRtp(Player player, CommandSender sender) {
        return checkCooldown(player, sender, Variables.getRuntimeState().getCooldowns(), Variables.isLoggingEnabled());
    }

    /**
     * Convenience wrapper: checks the biome RTP cooldown for a player.
     */
    public static boolean checkBiome(Player player, CommandSender sender) {
        return checkCooldown(player, sender, Variables.getRuntimeState().getBiomeCooldowns(), Variables.isLoggingEnabled());
    }

    private static int getCustomCooldown(Player player, int defaultCooldown, boolean loggingEnabled) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long[] cached = cooldownPermissionCache.get(playerId);
        if (cached != null && (now - cached[1]) < CACHE_TTL_MS) {
            int cooldown = cached[0] >= 0 ? (int) cached[0] : defaultCooldown;
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Applying cached cooldown: " + cooldown);
            }
            return cooldown;
        }

        Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
        for (PermissionAttachmentInfo permInfo : permissions) {
            if (permInfo.getValue()) {
                String permission = permInfo.getPermission();
                Matcher matcher = COOLDOWN_PERMISSION_PATTERN.matcher(permission);
                if (matcher.matches()) {
                    int cooldown = Integer.parseInt(matcher.group(1));
                    cooldownPermissionCache.put(playerId, new long[]{cooldown, now});
                    if (loggingEnabled) {
                        Bukkit.getConsoleSender().sendMessage(
                                "Matching permission: " + permission + ", extracted cooldown: " + cooldown);
                        Bukkit.getConsoleSender().sendMessage("Applying custom cooldown: " + cooldown);
                    }
                    return cooldown;
                }
            }
        }
        cooldownPermissionCache.put(playerId, new long[]{-1, now});
        if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage(
                    "No custom cooldown permissions found, using default: " + defaultCooldown);
        }
        return defaultCooldown;
    }
}
