package org.sRandomRTP.DifferentMethods.Teleport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TeleportRequestContextTest {

    private TeleportRequestContext context(long perAttemptMs, long totalMs) {
        return new TeleportRequestContext(UUID.randomUUID(), perAttemptMs, totalMs, true);
    }

    @Test
    void newContextIsNotCancelledOrCompleted() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertFalse(ctx.isCancelled());
        assertFalse(ctx.isCompleted());
        assertFalse(ctx.isInactive());
    }

    @Test
    void cancelSetsFlag() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertTrue(ctx.cancel("test"));
        assertTrue(ctx.isCancelled());
        assertTrue(ctx.isInactive());
    }

    @Test
    void cancelIsIdempotent() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertTrue(ctx.cancel("first"));
        assertFalse(ctx.cancel("second"));
    }

    @Test
    void markCompletedSetsFlag() {
        TeleportRequestContext ctx = context(5000, 30000);
        ctx.markCompleted();
        assertTrue(ctx.isCompleted());
        assertTrue(ctx.isInactive());
    }

    @Test
    void trackFutureOnCancelledContextCancelsFutureImmediately() {
        TeleportRequestContext ctx = context(5000, 30000);
        ctx.cancel("pre-cancel");
        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.trackFuture(future);
        assertTrue(future.isCancelled());
    }

    @Test
    void trackFutureOnCompletedContextCancelsFutureImmediately() {
        TeleportRequestContext ctx = context(5000, 30000);
        ctx.markCompleted();
        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.trackFuture(future);
        assertTrue(future.isCancelled());
    }

    @Test
    void cancelMakesContextInactiveSoSubsequentTrackFutureCancelsFutureImmediately() {
        TeleportRequestContext ctx = context(5000, 30000);
        ctx.cancel("cancel-before-track");
        // After cancellation, any new future tracked must be cancelled immediately
        CompletableFuture<Void> future = new CompletableFuture<>();
        ctx.trackFuture(future);
        assertTrue(future.isCancelled(), "future tracked after cancel should be cancelled immediately");
    }

    @Test
    void attemptCounterIncrements() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertEquals(1, ctx.incrementAttempt());
        assertEquals(2, ctx.incrementAttempt());
        assertEquals(2, ctx.getAttemptCount());
    }

    @Test
    void expiredWhenLifetimeExceeded() throws Exception {
        TeleportRequestContext ctx = new TeleportRequestContext(UUID.randomUUID(), 1, 1, false);
        Thread.sleep(5);
        assertTrue(ctx.isExpired());
        assertTrue(ctx.isInactive());
    }

    @Test
    void notExpiredWithinLifetime() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertFalse(ctx.isExpired());
    }

    @Test
    void recordMetricsDoNotThrow() {
        TeleportRequestContext ctx = context(5000, 30000);
        ctx.recordChunkAcquire(1000L, false, true);
        ctx.recordSafeSearch(500L);
        ctx.recordFinalTeleport(200L);
        assertTrue(ctx.getChunkAcquireNanos() > 0);
        assertTrue(ctx.getSafeSearchNanos() > 0);
        assertTrue(ctx.getFinalTeleportNanos() > 0);
    }

    @Test
    void generationFallbackActivatedOnce() {
        TeleportRequestContext ctx = context(5000, 30000);
        assertTrue(ctx.markGenerationFallbackActivated());
        assertFalse(ctx.markGenerationFallbackActivated());
        assertEquals(1, ctx.getFallbackToGenerationCount());
    }
}
