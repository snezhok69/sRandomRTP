package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkTicketManager {
    private static final long DEFAULT_HOLD_TICKS = 100L;
    private static final Map<String, AtomicInteger> ACTIVE_TICKETS = new ConcurrentHashMap<>();

    private ChunkTicketManager() {
    }

    public static void holdChunk(Location location) {
        holdChunk(location, DEFAULT_HOLD_TICKS);
    }

    public static void holdChunk(Location location, long ticks) {
        if (location == null) {
            return;
        }

        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        holdChunk(world, chunkX, chunkZ, ticks);
    }

    public static void holdChunk(World world, int chunkX, int chunkZ, long ticks) {
        if (world == null) {
            return;
        }

        Plugin plugin = Variables.getInstance();
        if (plugin == null) {
            return;
        }

        long delay = Math.max(ticks, 1L);
        String key = buildKey(world.getUID(), chunkX, chunkZ);
        AtomicInteger counter = ACTIVE_TICKETS.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        if (current == 1) {
            world.addPluginChunkTicket(chunkX, chunkZ, plugin);
        }

        Variables.getFoliaLib().getImpl().runLater(() -> releaseTicket(world, chunkX, chunkZ, key), delay);
    }

    private static void releaseTicket(World world, int chunkX, int chunkZ, String key) {
        Plugin plugin = Variables.getInstance();
        if (plugin == null) {
            return;
        }

        AtomicInteger counter = ACTIVE_TICKETS.get(key);
        if (counter == null) {
            return;
        }

        int remaining = counter.decrementAndGet();
        if (remaining <= 0) {
            ACTIVE_TICKETS.remove(key);
            world.removePluginChunkTicket(chunkX, chunkZ, plugin);
        }
    }

    private static String buildKey(UUID worldId, int chunkX, int chunkZ) {
        return worldId + ":" + chunkX + ':' + chunkZ;
    }
}
