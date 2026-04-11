package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunksToLoad {
    public static List<CompletableFuture<Chunk>> getChunksToLoad(Location location) {
        if (location == null || location.getWorld() == null) {
            return Collections.emptyList();
        }

        int configuredRadius = Variables.chunkfile.getInt("chunk-loading.preload-radius", 0);
        boolean chunkDebugLogs = Variables.chunkfile.getBoolean("chunk-loading.debug_logs",
                Variables.chunkfile.getBoolean("chunk-loading.debug-logs", false));

        if (configuredRadius > 0 && chunkDebugLogs) {
            Bukkit.getLogger().warning("[ChunkPreloader] Ignoring preload-radius=" + configuredRadius
                    + " and loading only the destination chunk");
        }

        CompletableFuture<Chunk> destinationChunk = AsyncChunkUtil.requestChunk(location);
        if (destinationChunk == null) {
            return Collections.emptyList();
        }

        return Collections.singletonList(destinationChunk);
    }

    public static List<CompletableFuture<Chunk>> getChunksToLoad(RtpCandidateResolution resolution) {
        if (resolution == null) {
            return Collections.emptyList();
        }

        Location targetLocation = resolution.toLocation();
        if (targetLocation == null) {
            return Collections.emptyList();
        }

        int configuredRadius = Variables.chunkfile.getInt("chunk-loading.preload-radius", 0);
        int effectiveRadius = Math.min(1, Math.max(0, configuredRadius));
        if (effectiveRadius <= 0) {
            return Collections.emptyList();
        }

        if (SearchPhasePolicy.shouldReduceChunkPressure()) {
            return Collections.emptyList();
        }

        boolean chunkDebugLogs = Variables.chunkfile.getBoolean("chunk-loading.debug_logs",
                Variables.chunkfile.getBoolean("chunk-loading.debug-logs", false));
        if (configuredRadius > effectiveRadius && chunkDebugLogs) {
            Bukkit.getLogger().warning("[ChunkPreloader] Limiting preload-radius=" + configuredRadius
                    + " to supported value " + effectiveRadius);
        }

        return ChunkAcquireService.preloadChunksAround(targetLocation, effectiveRadius);
    }


    @SuppressWarnings("deprecation")
    public static CompletableFuture<Boolean> waitForChunkLoads(List<CompletableFuture<Chunk>> chunkFutures,
                                                               Location location,
                                                               boolean loggingEnabled) {
        if (chunkFutures == null || chunkFutures.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Void> allChunksFuture = CompletableFuture.allOf(chunkFutures.toArray(new CompletableFuture[0]));

        long timeoutSeconds = 5L;
        if (Variables.chunkfile != null) {
            timeoutSeconds = Variables.chunkfile.getLong("chunk-loading.timeout-seconds", 5L);
        }

        if (timeoutSeconds < 1L) {
            timeoutSeconds = 1L;
        }

        long timeoutTicks = timeoutSeconds * 20L;
        AtomicBoolean timedOut = new AtomicBoolean(false);
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();

        Object timeoutHandle = Variables.getFoliaLib().getImpl().runAtLocationLater(location, task -> {
            if (allChunksFuture.isDone() || timeoutFuture.isDone()) {
                return;
            }

            if (!timeoutFuture.isDone()) {
                timedOut.set(true);
                timeoutFuture.complete(null);
            }
        }, timeoutTicks);

        WrappedTask timeoutTask = timeoutHandle instanceof WrappedTask ? (WrappedTask) timeoutHandle : null;
        CompletableFuture<?> timeoutScheduledFuture = timeoutHandle instanceof CompletableFuture<?> ? (CompletableFuture<?>) timeoutHandle : null;

        boolean chunkDebugLogsEnabled = Variables.chunkfile.getBoolean("chunk-loading.debug_logs",
                Variables.chunkfile.getBoolean("chunk-loading.debug-logs", false));
        boolean effectiveLogging = loggingEnabled || chunkDebugLogsEnabled;

        return CompletableFuture.anyOf(allChunksFuture, timeoutFuture)
                .handle((ignored, ex) -> {
                    if (!timedOut.get()) {
                        if (timeoutTask != null && !timeoutTask.isCancelled()) {
                            timeoutTask.cancel();
                        } else if (timeoutScheduledFuture != null && !timeoutScheduledFuture.isDone()) {
                            timeoutScheduledFuture.cancel(false);
                        }
                    }

                    if (ex != null) {
                        if (effectiveLogging) {
                            Bukkit.getLogger().severe("[ChunkPreloader] Error while waiting for chunks: " + ex.getMessage());
                        }
                        return true;
                    }

                    if (timedOut.get() && effectiveLogging) {
                        Bukkit.getLogger().warning("[ChunkPreloader] Chunk loading timeout reached, continuing with teleportation");
                    }

                    return timedOut.get();
                });
    }
}
