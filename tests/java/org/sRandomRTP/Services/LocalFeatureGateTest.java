package org.sRandomRTP.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFeatureGateTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
    }

    @Test
    void localAdminBarsAreDisabledByDefault() {
        assertFalse(LocalFeatureGate.isLocalAdminBarsEnabled());
    }

    @Test
    void localAdminBarsCanBeEnabledByJvmProperty() {
        System.setProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY, "true");

        assertTrue(LocalFeatureGate.isLocalAdminBarsEnabled());
    }

    @Test
    void truthyParserAcceptsExplicitLocalToggleValues() {
        assertTrue(LocalFeatureGate.isTruthy("1"));
        assertTrue(LocalFeatureGate.isTruthy("yes"));
        assertTrue(LocalFeatureGate.isTruthy("on"));
        assertFalse(LocalFeatureGate.isTruthy("false"));
        assertFalse(LocalFeatureGate.isTruthy(""));
    }
}
