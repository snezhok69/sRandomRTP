package org.sRandomRTP.Utils;

import io.papermc.lib.PaperLib;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Variables;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class AsyncChunkUtil {
    private static volatile Method asyncChunkMethod;
    private static volatile boolean paperMethodsResolved;
    private static final AtomicInteger INFLIGHT_CHUNK_REQUESTS = new AtomicInteger();

    private AsyncChunkUtil() {}

    public static CompletableFuture<Chunk> requestChunk(Location location) {
        if (location == null) {
            return CompletableFuture.completedFuture(null);
        }

        World world = location.getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        return requestChunk(world, location.getBlockX() >> 4, location.getBlockZ() >> 4, true);
    }

    public static CompletableFuture<Chunk> requestChunk(World world, int chunkX, int chunkZ) {
        return requestChunk(world, chunkX, chunkZ, true);
    }

    public static CompletableFuture<Chunk> requestChunk(World world, int chunkX, int chunkZ, boolean generate) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        return requestChunkDirect(world, chunkX, chunkZ, generate);
    }

    private static CompletableFuture<Chunk> requestChunkOnRegionThread(World world, int chunkX, int chunkZ) {
        CompletableFuture<Chunk> result = new CompletableFuture<>();
        long startedAt = System.nanoTime();
        try {
            Location chunkLocation = new Location(world, (chunkX << 4) + 8, world.getMinHeight(), (chunkZ << 4) + 8);
            FoliaSchedulerFacade.runAtLocation(chunkLocation, () -> {
                try {
                    Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                    result.complete(chunk);
                } catch (RuntimeException t) {
                    LoggerUtility.loggerUtility(AsyncChunkUtil.class, t);
                    result.completeExceptionally(t);
                }
            });
        } catch (RuntimeException t) {
            LoggerUtility.loggerUtility(AsyncChunkUtil.class, t);
            result.completeExceptionally(t);
        }
        return recordChunkTiming(result, startedAt);
    }

    private static CompletableFuture<Chunk> requestChunkDirect(World world, int chunkX, int chunkZ, boolean generate) {
        long startedAt = System.nanoTime();
        if (PaperLib.isPaper()) {
            resolvePaperMethods(world);
            try {
                if (asyncChunkMethod != null) {
                    Object fallback = asyncChunkMethod.invoke(world, chunkX, chunkZ, generate, true);
                    if (fallback instanceof CompletableFuture) {
                        @SuppressWarnings("unchecked")
                        CompletableFuture<Chunk> future = (CompletableFuture<Chunk>) fallback;
                        return recordChunkTiming(future, startedAt);
                    }
                }
            } catch (ReflectiveOperationException | IllegalArgumentException throwable) {
                LoggerUtility.loggerUtility(AsyncChunkUtil.class, throwable);
            }
        }
        try {
            return recordChunkTiming(PaperLib.getChunkAtAsync(world, chunkX, chunkZ, generate), startedAt);
        } catch (RuntimeException throwable) {
            LoggerUtility.loggerUtility(AsyncChunkUtil.class, throwable);
            return requestChunkOnRegionThread(world, chunkX, chunkZ);
        }
    }

    private static synchronized void resolvePaperMethods(World world) {
        if (paperMethodsResolved || world == null) {
            return;
        }
        try {
            asyncChunkMethod = world.getClass().getMethod("getChunkAtAsync", int.class, int.class, boolean.class, boolean.class);
        } catch (NoSuchMethodException ignored) {
            asyncChunkMethod = null;
        }
        paperMethodsResolved = true;
    }

    private static CompletableFuture<Chunk> recordChunkTiming(CompletableFuture<Chunk> future, long startedAt) {
        if (future == null) {
            return CompletableFuture.completedFuture(null);
        }
        INFLIGHT_CHUNK_REQUESTS.incrementAndGet();
        future.whenComplete((chunk, throwable) -> {
            INFLIGHT_CHUNK_REQUESTS.updateAndGet(current -> current <= 0 ? 0 : current - 1);
            if (Variables.getTeleportMetrics() != null) {
                Variables.getTeleportMetrics().recordChunkRequest(System.nanoTime() - startedAt);
            }
        });
        return future;
    }

    public static int getInflightChunkRequests() {
        return INFLIGHT_CHUNK_REQUESTS.get();
    }
}
