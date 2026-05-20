package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.Services.ConfigChangeReporter;
import org.sRandomRTP.Services.ConfigRegistry;
import org.sRandomRTP.Utils.ConfigValueParser;
import java.util.ArrayList;
import java.util.List;

public class ValidateConfigEntries {

    public static void validateManagedConfigs(ConfigRegistry registry) {
        if (registry == null) {
            return;
        }
        validateTeleportConfig(registry.getTeleportFile());
        validatePortalConfig(registry.getPortalFile());
        validateConfigEntries(registry.getTeleportFile());
    }

    public static void validateConfigEntries(FileConfiguration config) {
        if (config == null) {
            return;
        }

        List<String> bannedBlocks = config.getStringList("teleport.bannedBlocks");
        List<String> invalidBlocks = new ArrayList<>();
        for (String block : bannedBlocks) {
            Material material = ConfigValueParser.parseMaterial(block);
            if (material == null) {
                invalidBlocks.add(block);
            }
        }
        if (!invalidBlocks.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("Invalid banned blocks: " + String.join(", ", invalidBlocks));
            ConfigChangeReporter.record("Settings/teleport.yml", "invalid banned blocks", invalidBlocks);
        }

        List<String> bannedBiomes = config.getStringList("teleport.bannedBiomes");
        List<String> invalidBiomes = new ArrayList<>();
        for (String biome : bannedBiomes) {
            Biome parsedBiome = ConfigValueParser.parseBiome(biome);
            if (parsedBiome == null) {
                invalidBiomes.add(biome);
            }
        }
        if (!invalidBiomes.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("Invalid banned biomes: " + String.join(", ", invalidBiomes));
            ConfigChangeReporter.record("Settings/teleport.yml", "invalid banned biomes", invalidBiomes);
        }
    }

    private static void validateTeleportConfig(FileConfiguration teleport) {
        if (teleport == null) {
            return;
        }
        List<String> warnings = new ArrayList<>();
        int radius = teleport.getInt("teleport.radius", 0);
        int minRadius = teleport.getInt("teleport.minradius", 0);
        if (radius <= minRadius) {
            warnings.add("teleport.radius must be greater than teleport.minradius");
        }
        if (radius - minRadius > 0 && radius - minRadius < 50) {
            warnings.add("teleport.radius and teleport.minradius should differ by at least 50 blocks");
        }
        if (teleport.getInt("teleport.maxtries", 1) < 1) {
            warnings.add("teleport.maxtries must be at least 1");
        }
        String redirectWorld = teleport.getString("teleport.bannedworld.redirect.world", "");
        if (teleport.getBoolean("teleport.bannedworld.redirect.enabled", false)
                && (redirectWorld == null || redirectWorld.trim().isEmpty() || Bukkit.getWorld(redirectWorld) == null)) {
            warnings.add("teleport.bannedworld.redirect.world points to a missing world: " + redirectWorld);
        }
        reportWarnings("Settings/teleport.yml", warnings);
    }

    private static void validatePortalConfig(FileConfiguration portal) {
        if (portal == null) {
            return;
        }
        List<String> warnings = new ArrayList<>();
        validateMaterial(portal, "portal.materials.floor", warnings);
        validateMaterial(portal, "portal.materials.border", warnings);
        validateParticle(portal, "portal.particles.floor", warnings);
        validateParticle(portal, "portal.particles.border", warnings);
        validatePositive(portal, "portal.particles.floor_count", warnings);
        validatePositive(portal, "portal.particles.border_count", warnings);
        validatePositiveDouble(portal, "portal.particles.floor_density", warnings);
        validatePositiveDouble(portal, "portal.particles.border_density", warnings);
        String portalWorld = portal.getString("portal.rtp.world", "");
        if (portal.getBoolean("portal.rtp.enabled", false)
                && (portalWorld == null || portalWorld.trim().isEmpty() || Bukkit.getWorld(portalWorld) == null)) {
            warnings.add("portal.rtp.world points to a missing world: " + portalWorld);
        }
        reportWarnings("Settings/portal.yml", warnings);
    }

    private static void validateMaterial(FileConfiguration config, String path, List<String> warnings) {
        String value = config.getString(path);
        if (value != null && ConfigValueParser.parseMaterial(value) == null) {
            warnings.add(path + " has invalid material: " + value);
        }
    }

    private static void validateParticle(FileConfiguration config, String path, List<String> warnings) {
        String value = config.getString(path);
        if (value != null && ConfigValueParser.parseParticle(value) == null) {
            warnings.add(path + " has invalid particle: " + value);
        }
    }

    private static void validatePositive(FileConfiguration config, String path, List<String> warnings) {
        if (config.contains(path) && config.getInt(path, 1) < 0) {
            warnings.add(path + " must be 0 or greater");
        }
    }

    private static void validatePositiveDouble(FileConfiguration config, String path, List<String> warnings) {
        if (config.contains(path) && config.getDouble(path, 1.0D) <= 0.0D) {
            warnings.add(path + " must be greater than 0");
        }
    }

    private static void reportWarnings(String file, List<String> warnings) {
        if (warnings.isEmpty()) {
            return;
        }
        for (String warning : warnings) {
            Bukkit.getConsoleSender().sendMessage("[sRandomRTP] Config warning in " + file + ": " + warning);
        }
        ConfigChangeReporter.record(file, "validation warnings", warnings);
    }
}
