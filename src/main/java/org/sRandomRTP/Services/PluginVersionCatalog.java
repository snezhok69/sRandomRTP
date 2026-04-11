package org.sRandomRTP.Services;

/**
 * Central registry for plugin-related version constants.
 *
 * <p>This class exists to stop version values from being scattered across the codebase.
 * When a new plugin release requires a config migration, schema bump, or remote version
 * lookup change, update the values here first and let the rest of the code read from this
 * catalog instead of duplicating numbers in multiple services.</p>
 *
 * <p>What to change here during maintenance:</p>
 * <ul>
 *   <li>{@link #CONFIG_VERSION} when managed YAML structure/defaults change in a way that
 *   requires a migration pass.</li>
 *   <li>{@link #PORTAL_SCHEMA_VERSION} when the SQLite portal schema changes.</li>
 *   <li>{@link #REMOTE_VERSION_URL} if the authoritative online VERSION file moves.</li>
 *   <li>{@link #MIN_SUPPORTED_SERVER_MINOR} only when the minimum supported Minecraft server
 *   version changes.</li>
 * </ul>
 *
 * <p>What you normally do not need to change here:</p>
 * <ul>
 *   <li>The runtime plugin version shown by Bukkit/Paper. That still comes from the build
 *   metadata in {@code pom.xml} and {@code plugin.yml}.</li>
 *   <li>Per-file {@code config-version} markers inside resource templates. Runtime startup now
 *   synchronizes them automatically through {@link ConfigVersionSupport}, so they are no longer
 *   the source of truth.</li>
 * </ul>
 */
public final class PluginVersionCatalog {

    /**
     * Current managed configuration format version.
     *
     * <p>Bump this when defaults, key locations, or migration rules for managed YAML files
     * change and existing user files must be revisited.</p>
     */
    public static final int CONFIG_VERSION = 3;

    /**
     * Current SQLite schema version for the portal repository.
     *
     * <p>Bump this only when database structure changes, then add the corresponding migration
     * logic inside the portal repository.</p>
     */
    public static final int PORTAL_SCHEMA_VERSION = 2;

    /**
     * Authoritative URL that returns the latest published plugin version as a single line.
     */
    public static final String REMOTE_VERSION_URL =
            "https://gitlab.com/snezh0k69/sRandomRTP/-/raw/main/VERSION";

    /**
     * Minimum supported Minecraft minor version.
     *
     * <p>For example, {@code 16} means Minecraft {@code 1.16+}.</p>
     */
    public static final int MIN_SUPPORTED_SERVER_MINOR = 16;

    /**
     * Shared config path for managed YAML version markers.
     */
    public static final String CONFIG_VERSION_PATH = "config-version";

    private PluginVersionCatalog() {
    }
}
