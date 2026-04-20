package org.sRandomRTP.Utils;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * Shared utility methods for reading plugin configuration values.
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    /**
     * Reads a world-specific integer from {@code teleport.yml}, falling back to the global key.
     *
     * <p>Lookup order:
     * <ol>
     *   <li>{@code teleport.per-world.<worldName>.<key>} — per-world override</li>
     *   <li>{@code teleport.<key>} — global default</li>
     * </ol>
     *
     * @param teleportConfig the teleport.yml FileConfiguration (never null)
     * @param worldName      name of the current world
     * @param key            config key suffix (e.g., {@code "radius"}, {@code "minradius"})
     * @param fallback       value to return if neither path is present
     * @return the resolved int value
     */
    public static int getWorldSpecificInt(FileConfiguration teleportConfig,
                                          String worldName,
                                          String key,
                                          int fallback) {
        String perWorldPath = "teleport.per-world." + worldName + "." + key;
        if (teleportConfig.contains(perWorldPath)) {
            return teleportConfig.getInt(perWorldPath, fallback);
        }
        return teleportConfig.getInt("teleport." + key, fallback);
    }
}
