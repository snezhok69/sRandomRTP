package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TeleportRequestContext {

    private static final Logger LOG = Logger.getLogger(TeleportRequestContext.class.getName());

    /**
     * Single shared scheduler for all per-attempt timeouts — eliminates thread-pool-per-future leak.
     *
     * <p>This is the one intentional static thread pool in the plugin. It must fire independently
     * of Bukkit's scheduler (which stops during server shutdown), so a daemon-threaded
     * ScheduledExecutorService is the right choice here. It is shut down by
     * {@link #shutdownTimeoutScheduler()}, which is called from {@code Main.onDisable()}.
     */
    private static final java.util.concurrent.ScheduledExecutorService TIMEOUT_SCHEDULER =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "rtp-timeout");
                t.setDaemon(true);
                return t;
            });

    /** Called from Main.onDisable() to drain the shared timeout scheduler cleanly. */
    public static void shutdownTimeoutScheduler() {
        TIMEOUT_SCHEDULER.shutdown();
        try {
            // 500 ms is sufficient: this scheduler only holds lightweight timeout futures,
            // not heavy work. Waiting longer would stall server restart under load.
            if (!TIMEOUT_SCHEDULER.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                TIMEOUT_SCHEDULER.shutdownNow();
            }
        } catch (InterruptedException e) {
            TIMEOUT_SCHEDULER.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private final UUID requestId;
    private final UUID playerId;
    private final long createdAtMillis;
    private final long perAttemptTimeoutMillis;
    private final long maxLifetimeMillis;
    private final boolean enforcePerAttemptTimeout;

    private final AtomicInteger attemptCounter = new AtomicInteger();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicBoolean generationFallbackActivated = new AtomicBoolean(false);
    private final AtomicInteger generatedOnlyPhaseHits = new AtomicInteger();
    private final AtomicInteger parallelBatchesUsed = new AtomicInteger();
    private final AtomicInteger chunkReusedCount = new AtomicInteger();
    private final AtomicInteger chunkRegeneratedCount = new AtomicInteger();
    private final AtomicLong chunkAcquireNanos = new AtomicLong();
    private final AtomicLong safeSearchNanos = new AtomicLong();
    private final AtomicLong finalTeleportNanos = new AtomicLong();

    private volatile WrappedTask scheduledTask;
    private final java.util.Set<CompletableFuture<?>> pendingFutures = ConcurrentHashMap.newKeySet();

    TeleportRequestContext(UUID playerId,
                           long perAttemptTimeoutMillis,
                           long maxLifetimeMillis,
                           boolean enforcePerAttemptTimeout) {
        this.requestId = UUID.randomUUID();
        this.playerId = playerId;
        this.perAttemptTimeoutMillis = perAttemptTimeoutMillis;
        this.maxLifetimeMillis = maxLifetimeMillis;
        this.createdAtMillis = System.currentTimeMillis();
        this.enforcePerAttemptTimeout = enforcePerAttemptTimeout;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public int incrementAttempt() {
        return attemptCounter.incrementAndGet();
    }

    public int getAttemptCount() {
        return attemptCounter.get();
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public boolean isCompleted() {
        return completed.get();
    }

    public long getPerAttemptTimeoutMillis() {
        return perAttemptTimeoutMillis;
    }

    public long getElapsedMillis() {
        return Math.max(0L, System.currentTimeMillis() - createdAtMillis);
    }

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return (maxLifetimeMillis > 0 && now - createdAtMillis >= maxLifetimeMillis);
    }

    /**
     * Returns {@code true} if this context is no longer actionable — i.e. it has been
     * cancelled, completed, or its lifetime has expired.
     * Use this instead of the repetitive {@code isCancelled() || isCompleted() || isExpired()} pattern.
     */
    public boolean isInactive() {
        return isCancelled() || isCompleted() || isExpired();
    }

    public void markCompleted() {
        if (completed.compareAndSet(false, true)) {
            cancelScheduledTask();
            cancelPendingFuture();
        }
    }

    public boolean cancel(String reason) {
        if (cancelled.compareAndSet(false, true)) {
            if (reason != null && !reason.isEmpty()) {
                LOG.log(Level.FINE, () -> "[RTP] Cancel " + requestId + " for player " + playerId + ": " + reason);
            }
            cancelScheduledTask();
            cancelPendingFuture();
            return true;
        }
        return false;
    }

    public void trackTask(WrappedTask task) {
        cancelScheduledTask();

        if (task == null) {
            this.scheduledTask = null;
            return;
        }

        if (isCancelled() || isCompleted()) {
            try {
                if (!task.isCancelled()) {
                    task.cancel();
                }
            } catch (RuntimeException t) {
                LOG.log(Level.FINER, "[RTP] trackTask cancel error", t);
            }
            return;
        }

        this.scheduledTask = task;
    }

    public void trackFuture(CompletableFuture<?> future) {
        if (future == null) {
            return;
        }

        if (isCancelled() || isCompleted()) {
            future.cancel(false);
            return;
        }

        CompletableFuture<?> decorated = future;

        if (enforcePerAttemptTimeout && perAttemptTimeoutMillis > 0) {
            final CompletableFuture<?> target = future;

            final java.util.concurrent.ScheduledFuture<?> timeout =
                    TIMEOUT_SCHEDULER.schedule(() -> {
                        boolean fired = target.completeExceptionally(
                                new java.util.concurrent.TimeoutException(
                                        "Per-attempt timeout " + perAttemptTimeoutMillis + "ms"));
                        if (fired) {
                            LOG.log(Level.FINE, () -> "[RTP] Attempt timeout for " + requestId + " / player " + playerId);
                        }
                    }, perAttemptTimeoutMillis, TimeUnit.MILLISECONDS);

            decorated = target.whenComplete((r, ex) -> {
                try {
                    timeout.cancel(false);
                } catch (RuntimeException ignore) {
                }
            });
        }

        final CompletableFuture<?> trackedFuture = decorated;

        // Atomic check-then-add: synchronized with cancelPendingFuture() so that
        // a future cannot be added to pendingFutures AFTER cancel/complete has already
        // drained the set.  cancel(false) is called *outside* the lock to avoid running
        // completion callbacks while holding it (prevents re-entrant pendingFutures access).
        boolean shouldCancel;
        synchronized (this) {
            if (isCancelled() || isCompleted()) {
                shouldCancel = true;
            } else {
                pendingFutures.add(trackedFuture);
                shouldCancel = false;
            }
        }
        if (shouldCancel) {
            trackedFuture.cancel(false);
            return;
        }

        trackedFuture.whenComplete((result, throwable) -> pendingFutures.remove(trackedFuture));
    }

    public void recordChunkAcquire(long nanos, boolean reused, boolean generationAllowed) {
        if (nanos > 0L) {
            chunkAcquireNanos.addAndGet(nanos);
        }
        if (reused) {
            chunkReusedCount.incrementAndGet();
        }
        if (generationAllowed && !reused) {
            chunkRegeneratedCount.incrementAndGet();
        }
    }

    public void recordSafeSearch(long nanos) {
        if (nanos > 0L) {
            safeSearchNanos.addAndGet(nanos);
        }
    }

    public void recordFinalTeleport(long nanos) {
        if (nanos > 0L) {
            finalTeleportNanos.addAndGet(nanos);
        }
    }

    public long getChunkAcquireNanos() {
        return chunkAcquireNanos.get();
    }

    public long getSafeSearchNanos() {
        return safeSearchNanos.get();
    }

    public long getFinalTeleportNanos() {
        return finalTeleportNanos.get();
    }

    public int getGeneratedOnlyPhaseHits() {
        return generatedOnlyPhaseHits.get();
    }

    public void incrementGeneratedOnlyPhaseHits() {
        generatedOnlyPhaseHits.incrementAndGet();
    }

    public boolean markGenerationFallbackActivated() {
        return generationFallbackActivated.compareAndSet(false, true);
    }

    public int getFallbackToGenerationCount() {
        return generationFallbackActivated.get() ? 1 : 0;
    }

    public void incrementParallelBatchesUsed() {
        parallelBatchesUsed.incrementAndGet();
    }

    public int getParallelBatchesUsed() {
        return parallelBatchesUsed.get();
    }

    public int getChunkReusedCount() {
        return chunkReusedCount.get();
    }

    public int getChunkRegeneratedCount() {
        return chunkRegeneratedCount.get();
    }

    private void cancelScheduledTask() {
        WrappedTask task = this.scheduledTask;
        if (task != null && !task.isCancelled()) {
            try {
                task.cancel();
            } catch (RuntimeException t) {
                LOG.log(Level.FINER, "[RTP] cancelScheduledTask error", t);
            }
        }
        this.scheduledTask = null;
    }

    private void cancelPendingFuture() {
        // Drain pendingFutures under the same lock used by trackFuture() so that no
        // new future can slip in between the state-flag flip and this drain.
        // Cancellation is done *outside* the lock to prevent completion callbacks from
        // running while we hold it (avoids re-entrant pendingFutures operations).
        java.util.List<CompletableFuture<?>> snapshot;
        synchronized (this) {
            snapshot = new java.util.ArrayList<>(pendingFutures);
            pendingFutures.clear();
        }
        for (CompletableFuture<?> f : snapshot) {
            if (f != null && !f.isDone()) {
                try {
                    f.cancel(false);
                } catch (RuntimeException t) {
                    LOG.log(Level.FINER, "[RTP] cancelPendingFuture error", t);
                }
            }
        }
    }
}
