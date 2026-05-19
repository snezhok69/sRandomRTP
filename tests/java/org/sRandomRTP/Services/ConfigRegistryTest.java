package org.sRandomRTP.Services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRegistryTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
    }

    @Test
    void managedConfigsHideAdminBarsFileByDefault() {
        ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());

        boolean found = false;
        boolean foundBiome = false;
        for (File file : configRegistry.getManagedConfigFiles()) {
            if (file.getPath().endsWith("Settings" + File.separator + "admin-bars.yml")) {
                found = true;
            }
            if (file.getPath().endsWith("Settings" + File.separator + "biome.yml")) {
                foundBiome = true;
            }
        }

        assertFalse(found, "Settings/admin-bars.yml must stay out of public create/update/migration flow");
        assertTrue(foundBiome, "Settings/biome.yml must be managed for biome profile create/update flow");
    }

    @Test
    void managedConfigsIncludeAdminBarsFileWhenLocalGateEnabled() {
        System.setProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY, "true");
        try {
            ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());

            boolean found = false;
            for (File file : configRegistry.getManagedConfigFiles()) {
                if (file.getPath().endsWith("Settings" + File.separator + "admin-bars.yml")) {
                    found = true;
                }
            }

            assertTrue(found, "Settings/admin-bars.yml must be managed for local admin bossbar builds");
        } finally {
            System.clearProperty(LocalFeatureGate.ADMIN_BARS_PROPERTY);
        }
    }
}
