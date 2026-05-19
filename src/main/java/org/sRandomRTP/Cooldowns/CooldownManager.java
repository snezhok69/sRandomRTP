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
 *
 * <p>Can be used as a static utility (via {@link #instance()}) or instantiated
 * directly for isolated unit tests.
 */
public final class CooldownManager {

    private static final Pattern COOLDOWN_PERMISSION_PATTERN = Pattern.compile("(?i)sRandomRTP\\.Cooldown\\.(\\d+)");
    private static final long CACHE_TTL_MS = org.sRandomRTP.Utils.PluginConstants.COOLDOWN_PERMISSION_CACHE_TTL_MS;

    /** Immutable cache entry — thread-safe when published via ConcurrentHashMap. */
    private record CooldownCacheEntry(long cooldown, long cachedAt) {}

    /** Cache: player UUID → entry. Kept as a short-lived trace of the latest permission-derived value. */
    private final Map<UUID, CooldownCacheEntry> cooldownPermissionCache = new ConcurrentHashMap<>();

    /** Default singleton used by static convenience methods. */
    private static final CooldownManager INSTANCE = new CooldownManager();

    /** Returns the shared singleton instance. */
    public static CooldownManager instance() {
        return INSTANCE;
    }

    /** Invalidate cached permission lookup for a player (call on cooldown removal). */
    public void invalidatePermissionCache(UUID playerId) {
        cooldownPermissionCache.remove(playerId);
    }

    /** Evict cache entries older than TTL — call from a periodic cleanup task. */
    public void evictExpiredCache() {
        long now = System.currentTimeMillis();
        cooldownPermissionCache.entrySet().removeIf(e -> (now - e.getValue().cachedAt()) > CACHE_TTL_MS);
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
    public boolean checkCooldown(Player player, CommandSender sender,
                                 Map<UUID, Long> cooldownMap, boolean loggingEnabled) {
        UUID playerId = player.getUniqueId();
        if (Variables.configCache.cooldownsEnabled) {
            if (Permissions.hasCooldownBypass(player)) {
                // Update timestamp on bypass so cooldown counts from last teleport after bypass is revoked
                cooldownMap.put(playerId, System.currentTimeMillis());
                invalidatePermissionCache(playerId);
                return false;
            }

            int cooldown = Variables.configCache.defaultCooldown;

            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("Default cooldown from config: " + cooldown);
            }
            cooldown = resolveCustomCooldown(player, cooldown, loggingEnabled);
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
        org.sRandomRTP.Services.RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state == null) return false;
        return INSTANCE.checkCooldown(player, sender, state.getCooldowns(), Variables.isLoggingEnabled());
    }

    /**
     * Convenience wrapper: checks the biome RTP cooldown for a player.
     */
    public static boolean checkBiome(Player player, CommandSender sender) {
        org.sRandomRTP.Services.RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state == null) return false;
        return INSTANCE.checkCooldown(player, sender, state.getBiomeCooldowns(), Variables.isLoggingEnabled());
    }

    public int resolveCustomCooldown(Player player, int defaultCooldown, boolean loggingEnabled) {
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Set<PermissionAttachmentInfo> permissions = player.getEffectivePermissions();
        int customCooldown = -1;
        String matchedPermission = null;
        for (PermissionAttachmentInfo permInfo : permissions) {
            if (permInfo.getValue()) {
                String permission = permInfo.getPermission();
                Matcher matcher = COOLDOWN_PERMISSION_PATTERN.matcher(permission);
                if (matcher.matches()) {
                    int cooldown = Integer.parseInt(matcher.group(1));
                    if (customCooldown < 0 || cooldown < customCooldown) {
                        customCooldown = cooldown;
                        matchedPermission = permission;
                    }
                }
            }
        }
        if (customCooldown >= 0) {
            cooldownPermissionCache.put(playerId, new CooldownCacheEntry(customCooldown, now));
            if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage(
                        "Matching permission: " + matchedPermission + ", extracted cooldown: " + customCooldown);
                Bukkit.getConsoleSender().sendMessage("Applying custom cooldown: " + customCooldown);
            }
            return customCooldown;
        }
        cooldownPermissionCache.remove(playerId);
        if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage(
                    "No custom cooldown permissions found, using default: " + defaultCooldown);
        }
        return defaultCooldown;
    }
}
