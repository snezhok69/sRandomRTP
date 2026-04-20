package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class GetChunksToLoad {

    /**
     * Maximum supported preload radius.
     *
     * <p>Currently capped at 1 to keep async chunk-load pressure low.
     * Raising this would load (2r+1)² chunks per teleport, which can stall
     * the server under high concurrent RTP demand.</p>
     */
    private static final int MAX_PRELOAD_RADIUS = 1;

    public static List<CompletableFuture<Chunk>> getChunksToLoad(Location location) {
        if (location == null || location.getWorld() == null) {
            return Collections.emptyList();
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

        FileConfiguration chunkFile = Variables.getPluginContext().getConfigRegistry().getChunkFile();
        int configuredRadius = chunkFile != null ? chunkFile.getInt("chunk-loading.preload-radius", 0) : 0;
        int effectiveRadius  = Math.min(MAX_PRELOAD_RADIUS, Math.max(0, configuredRadius));
        if (effectiveRadius <= 0) {
            return Collections.emptyList();
        }

        if (SearchPhasePolicy.shouldReduceChunkPressure()) {
            return Collections.emptyList();
        }

        if (configuredRadius > effectiveRadius && isChunkDebugLogsEnabled()) {
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

        FileConfiguration chunkFile = Variables.getPluginContext().getConfigRegistry().getChunkFile();
        long timeoutSeconds = chunkFile != null ? Math.max(1L, chunkFile.getLong("chunk-loading.timeout-seconds", 5L)) : 5L;
        long timeoutTicks   = timeoutSeconds * 20L;

        AtomicBoolean timedOut = new AtomicBoolean(false);
        CompletableFuture<Void> timeoutFuture = new CompletableFuture<>();

        Object timeoutHandle = Variables.getFoliaLib().getImpl().runAtLocationLater(location, task -> {
            if (!allChunksFuture.isDone() && !timeoutFuture.isDone()) {
                timedOut.set(true);
                timeoutFuture.complete(null);
            }
        }, timeoutTicks);

        WrappedTask timeoutTask = timeoutHandle instanceof WrappedTask ? (WrappedTask) timeoutHandle : null;
        CompletableFuture<?> timeoutScheduledFuture = timeoutHandle instanceof CompletableFuture<?> ? (CompletableFuture<?>) timeoutHandle : null;

        boolean effectiveLogging = loggingEnabled || isChunkDebugLogsEnabled();

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

    /**
     * Returns {@code true} if chunk-loading debug logs are enabled in the config.
     * Supports both the current key ({@code debug_logs}) and the legacy key ({@code debug-logs}).
     */
    private static boolean isChunkDebugLogsEnabled() {
        FileConfiguration chunkFile = Variables.getPluginContext().getConfigRegistry().getChunkFile();
        if (chunkFile == null) return false;
        if (chunkFile.contains("chunk-loading.debug_logs")) {
            return chunkFile.getBoolean("chunk-loading.debug_logs", false);
        }
        return chunkFile.getBoolean("chunk-loading.debug-logs", false);
    }
}
