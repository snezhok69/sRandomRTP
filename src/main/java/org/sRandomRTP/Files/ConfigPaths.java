package org.sRandomRTP.Files;

/**
 * Canonical lists of plugin configuration file paths.
 *
 * <p>Both {@link FilesCreate} and {@link FilesUpdate} reference these constants so
 * that new config files only need to be registered in one place.</p>
 */
public final class ConfigPaths {

    private ConfigPaths() {}

    public static final String ADMIN_BARS_FILE = "Settings/admin-bars.yml";
    public static final String ADMIN_BARS_RU_FILE = "Settings_ru/admin-bars.yml";
    public static final String COMMANDS_FILE = "Settings/commands.yml";
    public static final String COMMANDS_RU_FILE = "Settings_ru/commands.yml";

    /**
     * Files that are both <em>created</em> on first run and <em>updated</em> on plugin upgrade.
     * Order is irrelevant.
     */
    public static final String[] UPDATABLE_FILES = {
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
            COMMANDS_FILE,
            ADMIN_BARS_FILE,
            "lang/en.yml",
            "lang/ru.yml",
            "lang/es.yml",
            "lang/de.yml",
            "lang/fr.yml",
            "lang/it.yml",
            "lang/pt.yml",
            "lang/zh.yml",
            "lang/ja.yml",
            "lang/ko.yml",
            "lang/ar.yml",
            "lang/pl.yml",
            "lang/vi.yml",
            "lang/ua.yml",
            "lang/tr.yml",
            "lang/custom_messages.yml",
    };

    /**
     * Files that are <em>only created</em> (not updated via {@link ConfigUpdater}) — typically
     * reference/documentation files and data files that must not be overwritten.
     */
    public static final String[] CREATE_ONLY_FILES = {
            "Data/rtpCount.yml",
            "Settings_ru/bossbar.yml",
            ADMIN_BARS_RU_FILE,
            "Settings_ru/chunk-loading.yml",
            COMMANDS_RU_FILE,
            "Settings_ru/config.yml",
            "Settings_ru/economy.yml",
            "Settings_ru/effects.yml",
            "Settings_ru/far.yml",
            "Settings_ru/middle.yml",
            "Settings_ru/biome.yml",
            "Settings_ru/near.yml",
            "Settings_ru/particles.yml",
            "Settings_ru/portal.yml",
            "Settings_ru/README.md",
            "Settings_ru/sound.yml",
            "Settings_ru/teleport.yml",
            "Settings_ru/title.yml",
    };
}
