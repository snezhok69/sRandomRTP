package org.sRandomRTP.DifferentMethods.Teleport;

import io.papermc.lib.PaperLib;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SearchPhasePolicy {

    private static final AtomicInteger GLOBAL_INFLIGHT_CANDIDATES = new AtomicInteger();
    private static final double INTERNAL_TPS_DRAG_THRESHOLD = 17.5D;
    private static final double INTERNAL_MSPT_DRAG_THRESHOLD = 40.0D;
    private static final int INTERNAL_CHUNK_PRESSURE_THRESHOLD = 12;

    private SearchPhasePolicy() {
    }

    public static boolean shouldUseParallelCandidateSearch() {
        FileConfiguration teleportConfig = Variables.teleportfile;
        if (teleportConfig == null || !PaperLib.isPaper()) {
            return false;
        }
        if (!teleportConfig.getBoolean("teleport.parallel-search.enabled", false)) {
            return false;
        }
        return teleportConfig.getInt("teleport.parallel-search.candidates-per-batch", 1) >= 2;
    }

    public static int resolveBatchSize(TeleportRequestContext context) {
        if (!shouldUseParallelCandidateSearch()) {
            return 1;
        }

        if (shouldReduceChunkPressure()) {
            return 1;
        }

        FileConfiguration teleportConfig = Variables.teleportfile;
        int configured = Math.max(1, teleportConfig.getInt("teleport.parallel-search.candidates-per-batch", 2));
        int maxBatch = Math.min(3, configured);
        int maxInflight = Math.max(1, teleportConfig.getInt("teleport.parallel-search.max-global-inflight", 24));
        int remainingCapacity = Math.max(0, maxInflight - GLOBAL_INFLIGHT_CANDIDATES.get());
        int effective = remainingCapacity <= 0 ? 1 : Math.min(maxBatch, remainingCapacity);

        if (context != null && effective > 1) {
            context.incrementParallelBatchesUsed();
        }
        return Math.max(1, effective);
    }

    public static boolean shouldPreferGeneratedChunks(TeleportRequestContext context, int attemptNumber) {
        FileConfiguration teleportConfig = Variables.teleportfile;
        if (teleportConfig == null || !PaperLib.isPaper()) {
            return false;
        }
        if (!teleportConfig.getBoolean("teleport.prefer-generated-chunks.enabled", false)) {
            return false;
        }
        if (context == null) {
            return false;
        }

        long windowMs = Math.max(0L, teleportConfig.getLong("teleport.prefer-generated-chunks.window-ms", 1000L));
        int maxAttempts = Math.max(1, teleportConfig.getInt("teleport.prefer-generated-chunks.max-attempts", 8));

        boolean withinWindow = context.getElapsedMillis() <= windowMs;
        boolean withinAttempts = attemptNumber <= maxAttempts;
        boolean preferGenerated = withinWindow && withinAttempts;

        if (preferGenerated) {
            context.incrementGeneratedOnlyPhaseHits();
        } else {
            context.markGenerationFallbackActivated();
        }

        return preferGenerated;
    }

    public static CandidatePermit acquireCandidatePermit(boolean primaryCandidate) {
        if (!primaryCandidate && shouldReduceChunkPressure()) {
            return CandidatePermit.unavailable();
        }

        FileConfiguration teleportConfig = Variables.teleportfile;
        int maxInflight = teleportConfig == null ? 24
                : Math.max(1, teleportConfig.getInt("teleport.parallel-search.max-global-inflight", 24));

        int updated = GLOBAL_INFLIGHT_CANDIDATES.incrementAndGet();
        if (updated > maxInflight) {
            GLOBAL_INFLIGHT_CANDIDATES.decrementAndGet();
            return primaryCandidate ? CandidatePermit.primaryNoop() : CandidatePermit.unavailable();
        }
        return CandidatePermit.acquired();
    }

    public static final class CandidatePermit {
        private static final CandidatePermit UNAVAILABLE = new CandidatePermit(false, false);
        private static final CandidatePermit PRIMARY_NOOP = new CandidatePermit(true, false);

        private final boolean usable;
        private final boolean acquired;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private CandidatePermit(boolean usable, boolean acquired) {
            this.usable = usable;
            this.acquired = acquired;
        }

        public static CandidatePermit acquired() {
            return new CandidatePermit(true, true);
        }

        public static CandidatePermit unavailable() {
            return UNAVAILABLE;
        }

        public static CandidatePermit primaryNoop() {
            return PRIMARY_NOOP;
        }

        public boolean isUsable() {
            return usable;
        }

        public void release() {
            if (!acquired || !released.compareAndSet(false, true)) {
                return;
            }
            GLOBAL_INFLIGHT_CANDIDATES.updateAndGet(current -> current <= 0 ? 0 : current - 1);
        }
    }

    public static boolean shouldReduceChunkPressure() {
        double currentTps = Variables.getServerMetricsProvider().getPrimaryTps();
        double currentMspt = Variables.getServerMetricsProvider().getAverageTickTimeMs();
        return shouldReduceChunkPressure(AsyncChunkUtil.getInflightChunkRequests(), currentTps, currentMspt);
    }

    static boolean shouldReduceChunkPressure(int inflightChunkRequests, double currentTps, double currentMspt) {
        if (inflightChunkRequests >= INTERNAL_CHUNK_PRESSURE_THRESHOLD) {
            return true;
        }

        if (!Double.isNaN(currentTps) && currentTps > 0.0D && currentTps < INTERNAL_TPS_DRAG_THRESHOLD) {
            return true;
        }

        return !Double.isNaN(currentMspt) && currentMspt > INTERNAL_MSPT_DRAG_THRESHOLD;
    }
}
