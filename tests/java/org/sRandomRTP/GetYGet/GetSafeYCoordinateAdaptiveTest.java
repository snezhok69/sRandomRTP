package org.sRandomRTP.GetYGet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetSafeYCoordinateAdaptiveTest {

    @Test
    void primaryColumnPassUsesCompactCandidateSet() {
        int[][] primaryColumns = GetSafeYCoordinate.buildCandidateColumns(100, 100, false);
        int[][] expandedColumns = GetSafeYCoordinate.buildCandidateColumns(100, 100, true);

        assertEquals(9, primaryColumns.length);
        assertTrue(expandedColumns.length > primaryColumns.length);
    }

    @Test
    void expandedColumnPassIsAllowedWithoutRequestContext() {
        assertTrue(GetSafeYCoordinate.shouldUseExpandedColumnPass(null));
    }
}
