package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.Commands.Permissions;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceMetadataTest {

    @Test
    void bundledConfigVersionsMatchCatalog() throws Exception {
        try (Stream<Path> stream = Files.walk(Paths.get("src/main/resources"))) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".yml"))
                    .filter(path -> !path.toString().contains("/lang/"))
                    .forEach(path -> {
                        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(path.toFile());
                        if (yaml.contains(PluginVersionCatalog.CONFIG_VERSION_PATH)) {
                            assertEquals(PluginVersionCatalog.CONFIG_VERSION,
                                    yaml.getInt(PluginVersionCatalog.CONFIG_VERSION_PATH),
                                    path + " has stale config-version");
                        }
                    });
        }
    }

    @Test
    void pluginYmlDeclaresAllPermissionConstants() throws Exception {
        YamlConfiguration pluginYml = YamlConfiguration.loadConfiguration(
                Paths.get("src/main/resources/plugin.yml").toFile());
        for (String permission : permissionConstants()) {
            assertTrue(pluginYml.contains("permissions." + permission),
                    "plugin.yml is missing permission: " + permission);
        }
    }

    @Test
    void compiledClassesRemainJava8Bytecode() throws Exception {
        byte[] mainClass = Files.readAllBytes(Paths.get("target/classes/org/sRandomRTP/Main.class"));
        int major = ((mainClass[6] & 0xFF) << 8) | (mainClass[7] & 0xFF);
        assertEquals(52, major, "compiled bytecode must stay Java 8 compatible");
    }

    private Set<String> permissionConstants() throws Exception {
        Set<String> permissions = new LinkedHashSet<>();
        for (Field field : Permissions.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && field.getType() == String.class) {
                permissions.add((String) field.get(null));
            }
        }
        return permissions;
    }
}
