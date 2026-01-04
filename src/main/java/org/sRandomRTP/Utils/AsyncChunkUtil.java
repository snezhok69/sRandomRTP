package org.sRandomRTP.Utils;

import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Variables;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class AsyncChunkUtil {
    private AsyncChunkUtil() {}

    public static CompletableFuture<Chunk> requestChunk(Location location) {
        if (location == null) {
            return CompletableFuture.completedFuture(null);
        }

        World world = location.getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        return requestChunk(world, location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public static CompletableFuture<Chunk> requestChunk(World world, int chunkX, int chunkZ) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            return requestChunkOnRegionThread(world, chunkX, chunkZ);
        }

        return requestChunkDirect(world, chunkX, chunkZ);
    }

    private static CompletableFuture<Chunk> requestChunkOnRegionThread(World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> result = new CompletableFuture<>();
        try {
            Location chunkLocation = new Location(world, (chunkX << 4) + 8, world.getMinHeight(), (chunkZ << 4) + 8);
            RegionTaskExecutor.runAtLocation(chunkLocation, () -> {
                try {
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    result.complete(chunk);
                } catch (Throwable t) {
                    LoggerUtility.loggerUtility(AsyncChunkUtil.class.getName(), t);
                    result.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            LoggerUtility.loggerUtility(AsyncChunkUtil.class.getName(), t);
            result.completeExceptionally(t);
        }
        return result;
    }

    private static CompletableFuture<Chunk> requestChunkDirect(World world, int chunkX, int chunkZ) {
        if (PaperLib.isPaper()) {
            try {
                Method urgentMethod = world.getClass().getMethod("getChunkAtAsyncUrgently", int.class, int.class);
                Object result = urgentMethod.invoke(world, chunkX, chunkZ);
                if (result instanceof CompletableFuture) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Chunk> future = (CompletableFuture<Chunk>) result;
                    return future;
                }
            } catch (NoSuchMethodException ignored) {
                try {
                    Method asyncMethod = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class, boolean.class);
                    Object fallback = asyncMethod.invoke(world, chunkX, chunkZ, true, true);
                    if (fallback instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Chunk> future = (CompletableFuture<Chunk>) fallback;
                        return future;
                    }
                } catch (NoSuchMethodException ignoredLegacy) {
                } catch (Throwable throwable) {
                    LoggerUtility.loggerUtility(AsyncChunkUtil.class.getName(), throwable);
                }
            } catch (Throwable throwable) {
                LoggerUtility.loggerUtility(AsyncChunkUtil.class.getName(), throwable);
            }
        }

        return PaperLib.getChunkAtAsync(world, chunkX, chunkZ, true);
    }
}
