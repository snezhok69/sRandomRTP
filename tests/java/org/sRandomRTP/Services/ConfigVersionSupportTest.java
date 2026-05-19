package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigVersionSupportTest {

    @TempDir
    Path tempDir;

    @Test
    void synchronizeManagedConfigVersionsUsesCatalogVersion() throws Exception {
        File configFile = new File(tempDir.toFile(), "config.yml");
        File teleportFile = new File(tempDir.toFile(), "Settings/teleport.yml");
        configFile.getParentFile().mkdirs();
        teleportFile.getParentFile().mkdirs();

        YamlConfiguration configYaml = new YamlConfiguration();
        configYaml.set("config-version", 1);
        configYaml.save(configFile);

        YamlConfiguration teleportYaml = new YamlConfiguration();
        teleportYaml.set("config-version", 1);
        teleportYaml.save(teleportFile);

        ConfigRegistry configRegistry = new ConfigRegistry(tempDir.toFile());
        ConfigVersionSupport support = new ConfigVersionSupport(configRegistry, Logger.getLogger("ConfigVersionSupportTest"));

        List<String> changedFiles = support.synchronizeManagedConfigVersions();

        assertTrue(changedFiles.contains("config.yml"));
        assertTrue(changedFiles.contains("Settings/teleport.yml"));
        assertEquals(PluginVersionCatalog.CONFIG_VERSION,
                YamlConfiguration.loadConfiguration(configFile).getInt("config-version"));
        assertEquals(PluginVersionCatalog.CONFIG_VERSION,
                YamlConfiguration.loadConfiguration(teleportFile).getInt("config-version"));
    }
}
