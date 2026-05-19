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
    void managedConfigsIncludeBiomeFile() {
        ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());

        boolean foundBiome = false;
        for (File file : configRegistry.getManagedConfigFiles()) {
            if (file.getPath().endsWith("Settings" + File.separator + "biome.yml")) {
                foundBiome = true;
            }
        }

        assertTrue(foundBiome, "Settings/biome.yml must be managed for biome profile create/update flow");
    }
}
