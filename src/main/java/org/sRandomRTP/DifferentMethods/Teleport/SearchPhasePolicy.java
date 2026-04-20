package org.sRandomRTP.DifferentMethods.Teleport;

import io.papermc.lib.PaperLib;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SearchPhasePolicy {

    /**
     * Cached once at class-load — PaperLib already computes this once internally, but avoids
     * repeated method calls. Guarded with try/catch so unit tests (no live Bukkit server)
     * do not cause ExceptionInInitializerError.
     */
    private static final boolean IS_PAPER = resolvePaper();

    private static boolean resolvePaper() {
        try {
            return PaperLib.isPaper();
        } catch (RuntimeException | ExceptionInInitializerError e) {
            return false; // unit-test / stub environment
        }
    }

    private static final AtomicInteger GLOBAL_INFLIGHT_CANDIDATES = new AtomicInteger();
    /**
     * Incremented on every {@link #reset()} call. Each {@link CandidatePermit} captures the epoch
     * at acquisition time. On {@link CandidatePermit#release()}, a permit whose epoch no longer
     * matches the current epoch is a no-op — it was created before the last reload and must not
     * decrement the post-reset counter.
     */
    private static final AtomicInteger RELOAD_EPOCH = new AtomicInteger(0);
    private static final double INTERNAL_TPS_DRAG_THRESHOLD = 17.5D;
    private static final double INTERNAL_MSPT_DRAG_THRESHOLD = 40.0D;
    private static final int INTERNAL_CHUNK_PRESSURE_THRESHOLD = 12;

    private SearchPhasePolicy() {
    }

    /**
     * Resets the global in-flight candidate counter to zero and advances the reload epoch.
     * Must be called from {@code CommandReload} after config is reloaded so that
     * any counter drift from pre-reload searches is cleared, and stale permits
     * issued before the reload do not decrement the freshly-zeroed counter.
     */
    public static void reset() {
        GLOBAL_INFLIGHT_CANDIDATES.set(0);
        RELOAD_EPOCH.incrementAndGet();
    }

    public static boolean shouldUseParallelCandidateSearch() {
        if (!IS_PAPER) {
            return false;
        }
        ConfigCache cfg = Variables.configCache;
        return cfg.parallelSearchEnabled && cfg.parallelSearchCandidatesPerBatch >= 2;
    }

    public static int resolveBatchSize(TeleportRequestContext context) {
        if (!shouldUseParallelCandidateSearch()) {
            return 1;
        }

        if (shouldReduceChunkPressure()) {
            return 1;
        }

        ConfigCache cfg = Variables.configCache;
        int maxBatch = Math.min(3, cfg.parallelSearchCandidatesPerBatch);
        int remainingCapacity = Math.max(0, cfg.parallelSearchMaxGlobalInflight - GLOBAL_INFLIGHT_CANDIDATES.get());
        int effective = remainingCapacity <= 0 ? 1 : Math.min(maxBatch, remainingCapacity);

        if (context != null && effective > 1) {
            context.incrementParallelBatchesUsed();
        }
        return Math.max(1, effective);
    }

    public static boolean shouldPreferGeneratedChunks(TeleportRequestContext context, int attemptNumber) {
        if (!IS_PAPER || context == null) {
            return false;
        }
        ConfigCache cfg = Variables.configCache;
        if (!cfg.preferGeneratedChunksEnabled) {
            return false;
        }

        boolean preferGenerated = context.getElapsedMillis() <= cfg.preferGeneratedChunksWindowMs
                && attemptNumber <= cfg.preferGeneratedChunksMaxAttempts;

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

        int maxInflight = Variables.configCache.parallelSearchMaxGlobalInflight;
        // Capture epoch before the atomic increment so the permit's epoch always matches
        // the generation in which it was issued.
        int epoch = RELOAD_EPOCH.get();

        // Atomically increment counter only if limit not exceeded (eliminates TOCTOU).
        int prev = GLOBAL_INFLIGHT_CANDIDATES.getAndUpdate(current -> current < maxInflight ? current + 1 : current);
        if (prev >= maxInflight) {
            return primaryCandidate ? CandidatePermit.primaryNoop() : CandidatePermit.unavailable();
        }
        return CandidatePermit.acquired(epoch);
    }

    public static final class CandidatePermit {
        private static final CandidatePermit UNAVAILABLE = new CandidatePermit(false, false, -1);
        private static final CandidatePermit PRIMARY_NOOP = new CandidatePermit(true, false, -1);

        private final boolean usable;
        private final boolean acquired;
        /** Epoch value at acquisition time; -1 for static singletons that are never released. */
        private final int epoch;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private CandidatePermit(boolean usable, boolean acquired, int epoch) {
            this.usable = usable;
            this.acquired = acquired;
            this.epoch = epoch;
        }

        public static CandidatePermit acquired(int epoch) {
            return new CandidatePermit(true, true, epoch);
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
            // Only decrement the counter if this permit was issued in the current reload epoch.
            // Stale permits (epoch < current) must not touch the post-reset counter.
            if (epoch == RELOAD_EPOCH.get()) {
                GLOBAL_INFLIGHT_CANDIDATES.updateAndGet(current -> current <= 0 ? 0 : current - 1);
            }
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
