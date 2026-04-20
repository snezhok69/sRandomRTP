package org.sRandomRTP.Commands.portal;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player portal teleport cooldowns.
 * Extracted from the static state in {@link org.sRandomRTP.Commands.CommandSetPortal}
 * to give cooldown management a single, focused home.
 *
 * <p>All operations are thread-safe via {@link ConcurrentHashMap}.</p>
 */
public final class PortalTeleportCooldownManager {

    private final ConcurrentHashMap<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Returns {@code true} if the player is currently on cooldown.
     * Expired entries are removed lazily on read.
     */
    public boolean isOnCooldown(UUID playerId) {
        Long expiresAt = cooldowns.get(playerId);
        if (expiresAt == null) return false;
        if (expiresAt <= System.currentTimeMillis()) {
            cooldowns.remove(playerId);
            return false;
        }
        return true;
    }

    /** Registers a cooldown for {@code playerId} expiring {@code durationMs} milliseconds from now. */
    public void setCooldown(UUID playerId, long durationMs) {
        cooldowns.put(playerId, System.currentTimeMillis() + durationMs);
    }

    /** Removes the cooldown for the given player. Safe to call even if no cooldown is set. */
    public void clearCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Removes all expired entries from the map.
     * Should be called periodically from a background task to prevent unbounded growth.
     */
    public void cleanExpired() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> e.getValue() <= now);
    }
}
