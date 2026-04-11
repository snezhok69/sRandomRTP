package org.sRandomRTP.DifferentMethods.Teleport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
