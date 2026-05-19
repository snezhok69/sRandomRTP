package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseCheckServiceTest {

    @Test
    void compareVersionsUsesNumericComponents() {
        assertEquals(0, ReleaseCheckService.compareVersions("3.0", "3.0.0"));
        assertEquals(1, Integer.signum(ReleaseCheckService.compareVersions("3.1", "3.0.9")));
        assertEquals(-1, Integer.signum(ReleaseCheckService.compareVersions("3.0.1", "3.1")));
    }
}
