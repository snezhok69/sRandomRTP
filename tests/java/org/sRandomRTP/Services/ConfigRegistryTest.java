package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void managedConfigsIncludeAdminBarsFileByDefault() {
        ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());

        boolean foundAdminBars = false;
        boolean foundBiome = false;
        for (File file : configRegistry.getManagedConfigFiles()) {
            if (file.getPath().endsWith("Settings" + File.separator + "admin-bars.yml")) {
                foundAdminBars = true;
            }
            if (file.getPath().endsWith("Settings" + File.separator + "biome.yml")) {
                foundBiome = true;
            }
        }

        assertTrue(foundAdminBars, "Settings/admin-bars.yml must be managed in public builds");
        assertTrue(foundBiome, "Settings/biome.yml must be managed for biome profile create/update flow");
    }
}
