package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.Main;
import org.sRandomRTP.DifferentMethods.Variables;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigRegistry {

    private static final String[] MANAGED_CONFIG_PATHS = {
            "config.yml",
            "Settings/effects.yml",
            "Settings/particles.yml",
            "Settings/teleport.yml",
            "Settings/near.yml",
            "Settings/sound.yml",
            "Settings/title.yml",
            "Settings/bossbar.yml",
            "Settings/economy.yml",
            "Settings/far.yml",
            "Settings/middle.yml",
            "Settings/biome.yml",
            "Settings/portal.yml",
            "Settings/chunk-loading.yml",
            "Settings/admin-bars.yml"
    };

    private final File dataFolder;

    public ConfigRegistry(Main plugin) {
        this(plugin.getDataFolder());
    }

    public ConfigRegistry(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void reload() {
        Variables.effectfile = load("Settings/effects.yml");
        Variables.particlesfile = load("Settings/particles.yml");
        Variables.teleportfile = load("Settings/teleport.yml");
        Variables.bossbarfile = load("Settings/bossbar.yml");
        Variables.nearfile = load("Settings/near.yml");
        Variables.titlefile = load("Settings/title.yml");
        Variables.economyfile = load("Settings/economy.yml");
        Variables.soundfile = load("Settings/sound.yml");
        Variables.farfile = load("Settings/far.yml");
        Variables.middlefile = load("Settings/middle.yml");
        Variables.biomefile = load("Settings/biome.yml");
        Variables.portalfile = load("Settings/portal.yml");
        Variables.chunkfile = load("Settings/chunk-loading.yml");
        Variables.adminbarsfile = load("Settings/admin-bars.yml");
    }

    public File resolve(String relativePath) {
        return new File(dataFolder, relativePath);
    }

    public List<File> getManagedConfigFiles() {
        List<File> files = new ArrayList<>();
        for (String path : MANAGED_CONFIG_PATHS) {
            files.add(resolve(path));
        }
        return Collections.unmodifiableList(files);
    }

    public File getDataFolder() {
        return dataFolder;
    }

    private YamlConfiguration load(String relativePath) {
        return YamlConfiguration.loadConfiguration(resolve(relativePath));
    }
}
