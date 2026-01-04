package org.sRandomRTP.DifferentMethods.Teleport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class RecentTeleportCache {
    private final Map<UUID, Deque<Long>> recentLocations = new ConcurrentHashMap<>();
    private final int maxEntries;

    RecentTeleportCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    boolean isRecentlyUsed(UUID playerId, int chunkX, int chunkZ) {
        Deque<Long> deque = recentLocations.get(playerId);
        if (deque == null) {
            return false;
        }
        long key = toKey(chunkX, chunkZ);
        return deque.contains(key);
    }

    void remember(UUID playerId, int chunkX, int chunkZ) {
        recentLocations.compute(playerId, (uuid, deque) -> {
            if (deque == null) {
                deque = new ArrayDeque<>(maxEntries);
            }
            long key = toKey(chunkX, chunkZ);
            if (deque.contains(key)) {
                return deque;
            }
            if (deque.size() >= maxEntries) {
                deque.removeFirst();
            }
            deque.addLast(key);
            return deque;
        });
    }

    void clear(UUID playerId) {
        recentLocations.remove(playerId);
    }

    private long toKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }
}
