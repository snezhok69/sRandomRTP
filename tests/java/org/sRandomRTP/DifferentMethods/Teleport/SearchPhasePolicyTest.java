package org.sRandomRTP.DifferentMethods.Teleport;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class SearchPhasePolicyTest {

    @Test
    void reducesPressureWhenInflightChunksAreHigh() {
        assertTrue(SearchPhasePolicy.shouldReduceChunkPressure(12, 20.0D, 10.0D));
    }

    @Test
    void reducesPressureWhenTpsIsLow() {
        assertTrue(SearchPhasePolicy.shouldReduceChunkPressure(0, 17.0D, 10.0D));
    }

    @Test
    void reducesPressureWhenMsptIsHigh() {
        assertTrue(SearchPhasePolicy.shouldReduceChunkPressure(0, 20.0D, 45.0D));
    }

    @Test
    void keepsPressureOpenWhenServerLooksHealthy() {
        assertFalse(SearchPhasePolicy.shouldReduceChunkPressure(0, 19.8D, 15.0D));
    }

    /**
     * Regression test for Bug #2:
     * A CandidatePermit acquired BEFORE a {@code reset()} call must NOT decrement
     * the post-reset counter when {@code release()} is called later.
     * Without the epoch mechanism the counter could drift below zero and permanently
     * under-count in-flight searches after every reload.
     */
    @Test
    void stalePermitAfterResetDoesNotDecrementCounter() throws Exception {
        AtomicInteger inflightField = getStaticAtomicInteger(SearchPhasePolicy.class, "GLOBAL_INFLIGHT_CANDIDATES");
        AtomicInteger epochField    = getStaticAtomicInteger(SearchPhasePolicy.class, "RELOAD_EPOCH");

        int savedInflight = inflightField.get();
        int savedEpoch    = epochField.get();

        try {
            inflightField.set(1);
            int preResetEpoch = epochField.get();
            // Create a permit as if it was issued in the current (pre-reset) epoch
            SearchPhasePolicy.CandidatePermit stalePermit =
                    SearchPhasePolicy.CandidatePermit.acquired(preResetEpoch);

            // Reload fires — counter resets and epoch advances
            SearchPhasePolicy.reset();
            assertEquals(0, inflightField.get(), "counter should be 0 after reset()");
            assertEquals(preResetEpoch + 1, epochField.get(), "epoch should have incremented");

            // The stale permit is released — must NOT decrement below 0
            stalePermit.release();
            assertEquals(0, inflightField.get(),
                    "stale permit must not decrement post-reset counter");
        } finally {
            inflightField.set(savedInflight);
            epochField.set(savedEpoch);
        }
    }

    /**
     * Verifies the happy path: a permit issued in the current epoch correctly
     * decrements the counter by 1 when released.
     */
    @Test
    void currentEpochPermitDecrementsCounterOnRelease() throws Exception {
        AtomicInteger inflightField = getStaticAtomicInteger(SearchPhasePolicy.class, "GLOBAL_INFLIGHT_CANDIDATES");
        AtomicInteger epochField    = getStaticAtomicInteger(SearchPhasePolicy.class, "RELOAD_EPOCH");

        int savedInflight = inflightField.get();
        int savedEpoch    = epochField.get();

        try {
            inflightField.set(2);
            int currentEpoch = epochField.get();
            SearchPhasePolicy.CandidatePermit permit =
                    SearchPhasePolicy.CandidatePermit.acquired(currentEpoch);

            permit.release();
            assertEquals(1, inflightField.get(),
                    "current-epoch permit must decrement counter by 1 on release");
        } finally {
            inflightField.set(savedInflight);
            epochField.set(savedEpoch);
        }
    }

    /** Releasing a permit twice must be a no-op on the second call (AtomicBoolean guard). */
    @Test
    void permitReleaseIsIdempotent() throws Exception {
        AtomicInteger inflightField = getStaticAtomicInteger(SearchPhasePolicy.class, "GLOBAL_INFLIGHT_CANDIDATES");
        AtomicInteger epochField    = getStaticAtomicInteger(SearchPhasePolicy.class, "RELOAD_EPOCH");

        int savedInflight = inflightField.get();
        int savedEpoch    = epochField.get();

        try {
            inflightField.set(3);
            int currentEpoch = epochField.get();
            SearchPhasePolicy.CandidatePermit permit =
                    SearchPhasePolicy.CandidatePermit.acquired(currentEpoch);

            permit.release();
            permit.release(); // second release must be a no-op
            assertEquals(2, inflightField.get(),
                    "double-release must not decrement counter twice");
        } finally {
            inflightField.set(savedInflight);
            epochField.set(savedEpoch);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static AtomicInteger getStaticAtomicInteger(Class<?> clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (AtomicInteger) field.get(null);
    }
}
