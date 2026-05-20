package org.sRandomRTP.Services;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.sRandomRTP.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns all managed {@link FileConfiguration} instances.
 *
 * <p>Internally backed by a single {@code volatile Map} that is atomically
 * swapped on every {@link #reload()} call — no per-field volatiles needed.
 * Adding a new public config file only requires two changes: add the path to
 * {@link #PUBLIC_MANAGED_CONFIG_PATHS} and add a typed getter.</p>
 *
 * <p>{@link Variables} fields delegate here for backward compatibility
 * during the ongoing migration.</p>
 */
public class ConfigRegistry {

    // ── Path constants ───────────────────────────────────────────────────────
    private static final String PATH_EFFECTS    = "Settings/effects.yml";
    private static final String PATH_PARTICLES  = "Settings/particles.yml";
    private static final String PATH_TELEPORT   = "Settings/teleport.yml";
    private static final String PATH_BOSSBAR    = "Settings/bossbar.yml";
    private static final String PATH_NEAR       = "Settings/near.yml";
    private static final String PATH_TITLE      = "Settings/title.yml";
    private static final String PATH_ECONOMY    = "Settings/economy.yml";
    private static final String PATH_SOUND      = "Settings/sound.yml";
    private static final String PATH_FAR        = "Settings/far.yml";
    private static final String PATH_MIDDLE     = "Settings/middle.yml";
    private static final String PATH_BIOME      = "Settings/biome.yml";
    private static final String PATH_PORTAL     = "Settings/portal.yml";
    private static final String PATH_CHUNK      = "Settings/chunk-loading.yml";
    private static final String PATH_COMMANDS   = "Settings/commands.yml";
    private static final String PATH_ADMIN_BARS = "Settings/admin-bars.yml";

    private static final String[] PUBLIC_MANAGED_CONFIG_PATHS = {
            "config.yml",
            PATH_EFFECTS,
            PATH_PARTICLES,
            PATH_TELEPORT,
            PATH_NEAR,
            PATH_SOUND,
            PATH_TITLE,
            PATH_BOSSBAR,
            PATH_ECONOMY,
            PATH_FAR,
            PATH_MIDDLE,
            PATH_BIOME,
            PATH_PORTAL,
            PATH_CHUNK,
            PATH_COMMANDS,
            PATH_ADMIN_BARS,
    };

    private final File dataFolder;

    /** Atomically swapped on every reload — readers always see a consistent snapshot. */
    private volatile Map<String, FileConfiguration> configs = Collections.emptyMap();

    public ConfigRegistry(Main plugin) {
        this(plugin.getDataFolder());
    }

    public ConfigRegistry(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void reload() {
        List<String> managedPaths = getManagedConfigPaths();
        Map<String, FileConfiguration> fresh = new HashMap<>(managedPaths.size() * 2);
        for (String path : managedPaths) {
            fresh.put(path, load(path));
        }
        configs = fresh;
    }

    // ── Typed getters ────────────────────────────────────────────────────────
    public FileConfiguration getEffectFile()    { return configs.get(PATH_EFFECTS); }
    public FileConfiguration getParticlesFile() { return configs.get(PATH_PARTICLES); }
    public FileConfiguration getTeleportFile()  { return configs.get(PATH_TELEPORT); }
    public FileConfiguration getBossBarFile()   { return configs.get(PATH_BOSSBAR); }
    public FileConfiguration getNearFile()      { return configs.get(PATH_NEAR); }
    public FileConfiguration getTitleFile()     { return configs.get(PATH_TITLE); }
    public FileConfiguration getEconomyFile()   { return configs.get(PATH_ECONOMY); }
    public FileConfiguration getSoundFile()     { return configs.get(PATH_SOUND); }
    public FileConfiguration getFarFile()       { return configs.get(PATH_FAR); }
    public FileConfiguration getMiddleFile()    { return configs.get(PATH_MIDDLE); }
    public FileConfiguration getBiomeFile()     { return configs.get(PATH_BIOME); }
    public FileConfiguration getPortalFile()    { return configs.get(PATH_PORTAL); }
    public FileConfiguration getChunkFile()     { return configs.get(PATH_CHUNK); }
    public FileConfiguration getCommandsFile()  { return configs.get(PATH_COMMANDS); }
    public FileConfiguration getAdminBarsFile() { return configs.get(PATH_ADMIN_BARS); }
    /** Returns the main {@code config.yml} configuration. */
    public FileConfiguration getMainConfig()    { return configs.get("config.yml"); }

    public File resolve(String relativePath) {
        return new File(dataFolder, relativePath);
    }

    public List<File> getManagedConfigFiles() {
        List<String> managedPaths = getManagedConfigPaths();
        List<File> files = new ArrayList<>(managedPaths.size());
        for (String path : managedPaths) {
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

    private List<String> getManagedConfigPaths() {
        return new ArrayList<>(Arrays.asList(PUBLIC_MANAGED_CONFIG_PATHS));
    }
}
