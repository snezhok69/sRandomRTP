package org.sRandomRTP.DifferentMethods.Teleport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class RecentTeleportCache {
    private static final class Entry {
        final Deque<Long> order = new ArrayDeque<>();
        final Set<Long> lookup = new HashSet<>();
    }

    private final Map<UUID, Entry> recentLocations = new ConcurrentHashMap<>();
    private final int maxEntries;

    RecentTeleportCache(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
    }

    boolean isRecentlyUsed(UUID playerId, int chunkX, int chunkZ) {
        Entry entry = recentLocations.get(playerId);
        if (entry == null) {
            return false;
        }
        long key = toKey(chunkX, chunkZ);
        return entry.lookup.contains(key);
    }

    void remember(UUID playerId, int chunkX, int chunkZ) {
        recentLocations.compute(playerId, (uuid, entry) -> {
            if (entry == null) {
                entry = new Entry();
            }
            long key = toKey(chunkX, chunkZ);
            if (entry.lookup.contains(key)) {
                return entry;
            }
            if (entry.order.size() >= maxEntries) {
                Long evicted = entry.order.removeFirst();
                entry.lookup.remove(evicted);
            }
            entry.order.addLast(key);
            entry.lookup.add(key);
            return entry;
        });
    }

    void clear(UUID playerId) {
        recentLocations.remove(playerId);
    }

    private long toKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
    }
}
