package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Synchronizes managed config file version markers from {@link PluginVersionCatalog}.
 *
 * <p>This class makes runtime config versioning deterministic and removes the need to manually
 * edit {@code config-version} in every managed YAML resource whenever a new migration version is
 * introduced. The effective version comes from {@link PluginVersionCatalog#CONFIG_VERSION}; the
 * synchronizer simply ensures that all managed user configs on disk reflect that value.</p>
 *
 * <p>Typical usage:</p>
 * <ol>
 *   <li>Create missing files from bundled resources.</li>
 *   <li>Run migrations for old configs.</li>
 *   <li>Update files from templates.</li>
 *   <li>Call {@link #synchronizeManagedConfigVersions()} to stamp the current version marker.</li>
 * </ol>
 *
 * <p>To introduce a new config migration release, you usually only need to:</p>
 * <ol>
 *   <li>Bump {@link PluginVersionCatalog#CONFIG_VERSION}.</li>
 *   <li>Add the new migration rules in {@link MigrationRunner}.</li>
 * </ol>
 */
public class ConfigVersionSupport {

    private final ConfigRegistry configRegistry;
    private final Logger logger;

    public ConfigVersionSupport(ConfigRegistry configRegistry, Logger logger) {
        this.configRegistry = configRegistry;
        this.logger = logger;
    }

    public List<String> synchronizeManagedConfigVersions() {
        if (configRegistry == null) {
            return Collections.emptyList();
        }

        List<String> changedFiles = new ArrayList<String>();
        for (File file : configRegistry.getManagedConfigFiles()) {
            if (file == null || !file.exists()) {
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            int currentVersion = yaml.getInt(PluginVersionCatalog.CONFIG_VERSION_PATH, 0);
            if (currentVersion == PluginVersionCatalog.CONFIG_VERSION) {
                continue;
            }

            yaml.set(PluginVersionCatalog.CONFIG_VERSION_PATH, PluginVersionCatalog.CONFIG_VERSION);
            try {
                yaml.save(file);
                changedFiles.add(toRelativePath(file));
            } catch (IOException e) {
                if (logger != null) {
                    logger.warning("Failed to synchronize config version for " + file.getName() + ": " + e.getMessage());
                }
            }
        }
        return changedFiles;
    }

    private String toRelativePath(File file) {
        try {
            File dataFolder = configRegistry.getDataFolder();
            if (dataFolder == null) {
                return file.getPath();
            }
            Path base = dataFolder.toPath();
            Path target = file.toPath();
            return base.relativize(target).toString().replace(File.separatorChar, '/');
        } catch (RuntimeException ignored) {
            return file.getPath();
        }
    }
}
