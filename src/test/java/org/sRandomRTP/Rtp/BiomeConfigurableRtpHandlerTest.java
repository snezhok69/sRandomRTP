package org.sRandomRTP.Rtp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeConfigurableRtpHandlerTest {

    @Test
    void staysInFastRandomStageUntilThresholdIsPassed() {
        assertFalse(BiomeConfigurableRtpHandler.isBiomeScanStage("TWO_PHASE", 1, 12));
        assertFalse(BiomeConfigurableRtpHandler.isBiomeScanStage("TWO_PHASE", 12, 12));
        assertTrue(BiomeConfigurableRtpHandler.isBiomeScanStage("TWO_PHASE", 13, 12));
    }

    @Test
    void disablesBiomeScanWhenSearchModeIsNotTwoPhase() {
        assertFalse(BiomeConfigurableRtpHandler.isBiomeScanStage("FAST_ONLY", 50, 12));
        assertEquals(1, BiomeConfigurableRtpHandler.resolveProbeSamples("FAST_ONLY", 50, 12, 8));
    }

    @Test
    void usesConfiguredProbeSamplesInBiomeScanStage() {
        assertEquals(1, BiomeConfigurableRtpHandler.resolveProbeSamples("TWO_PHASE", 5, 12, 8));
        assertEquals(8, BiomeConfigurableRtpHandler.resolveProbeSamples("TWO_PHASE", 20, 12, 8));
    }
}
