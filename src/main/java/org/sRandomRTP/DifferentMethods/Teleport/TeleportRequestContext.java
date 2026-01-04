package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TeleportRequestContext {

    private static final Logger LOG = Logger.getLogger(TeleportRequestContext.class.getName());

    private final UUID requestId;
    private final UUID playerId;
    private final long createdAtMillis;
    private final long perAttemptTimeoutMillis;
    private final long maxLifetimeMillis;
    private final boolean enforcePerAttemptTimeout;

    private final AtomicInteger attemptCounter = new AtomicInteger();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);

    private volatile WrappedTask scheduledTask;
    private volatile CompletableFuture<?> pendingFuture;

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

    public boolean isExpired() {
        long now = System.currentTimeMillis();
        return (maxLifetimeMillis > 0 && now - createdAtMillis >= maxLifetimeMillis);
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
            if (!task.isCancelled()) {
                task.cancel();
            }
            return;
        }

        this.scheduledTask = task;
    }

    public void trackFuture(CompletableFuture<?> future) {
        cancelPendingFuture();

        if (future == null) {
            this.pendingFuture = null;
            return;
        }

        if (isCancelled() || isCompleted()) {
            future.cancel(false);
            this.pendingFuture = null;
            return;
        }

        CompletableFuture<?> decorated = future;

        if (enforcePerAttemptTimeout && perAttemptTimeoutMillis > 0) {
            final CompletableFuture<?> target = future;

            final java.util.concurrent.ScheduledExecutorService ses =
                    java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread t = new Thread(r, "rtp-timeout-" + requestId);
                        t.setDaemon(true);
                        return t;
                    });

            final java.util.concurrent.ScheduledFuture<?> timeout =
                    ses.schedule(() -> {
                        boolean fired = target.completeExceptionally(
                                new java.util.concurrent.TimeoutException(
                                        "Per-attempt timeout " + perAttemptTimeoutMillis + "ms"));
                        if (fired) {
                            LOG.log(Level.FINE, () -> "[RTP] Attempt timeout for " + requestId + " / player " + playerId);
                        }
                    }, perAttemptTimeoutMillis, TimeUnit.MILLISECONDS);

            decorated = target.whenComplete((r, ex) -> {
                try { timeout.cancel(false); } catch (Throwable ignore) {}
                try { ses.shutdown(); } catch (Throwable ignore) {}
            });
        }

        this.pendingFuture = decorated;
    }

    private void cancelScheduledTask() {
        WrappedTask task = this.scheduledTask;
        if (task != null && !task.isCancelled()) {
            try {
                task.cancel();
            } catch (Throwable t) {
                LOG.log(Level.FINER, "[RTP] cancelScheduledTask error", t);
            }
        }
        this.scheduledTask = null;
    }

    private void cancelPendingFuture() {
        CompletableFuture<?> future = this.pendingFuture;
        if (future != null && !future.isDone()) {
            try {
                future.cancel(false);
            } catch (Throwable t) {
                LOG.log(Level.FINER, "[RTP] cancelPendingFuture error", t);
            }
        }
        this.pendingFuture = null;
    }
}
