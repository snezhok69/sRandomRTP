package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginVersionCatalogTest {

    @Test
    void catalogExposesPositiveVersionConstants() {
        assertTrue(PluginVersionCatalog.CONFIG_VERSION > 0);
        assertTrue(PluginVersionCatalog.PORTAL_SCHEMA_VERSION > 0);
        assertTrue(PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR >= 16);
    }
}
