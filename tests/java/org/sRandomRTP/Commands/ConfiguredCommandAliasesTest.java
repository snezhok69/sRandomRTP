package org.sRandomRTP.Commands;

import org.junit.jupiter.api.Test;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguredCommandAliasesTest {

    @Test
    void sanitizeAliasesNormalizesAndFiltersConfiguredValues() {
        assertEquals(
                Arrays.asList("randomtp", "ok_1", "ok-2", "admin"),
                ConfiguredCommandAliases.sanitizeAliases(Arrays.asList(
                        " randomtp ",
                        "/RANDOMTP",
                        "rtp",
                        "",
                        "bad alias",
                        "ok_1",
                        "ok-2",
                        "Admin"
                )));
    }

    @Test
    void sanitizeAliasesAcceptsEmptyConfig() {
        assertEquals(Collections.emptyList(), ConfiguredCommandAliases.sanitizeAliases(Collections.<String>emptyList()));
    }

    @Test
    void configuredAliasesCanBeDisabledWithQuickSwitch() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(ConfiguredCommandAliases.ENABLED_CONFIG_PATH, false);
        config.set(ConfiguredCommandAliases.CONFIG_PATH, Arrays.asList("randomtp", "randomteleport"));

        assertEquals(Collections.emptyList(), ConfiguredCommandAliases.getConfiguredAliases(config));
    }
}
