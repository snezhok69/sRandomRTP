package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.Utils.ConfigValueParser;
import java.util.ArrayList;
import java.util.List;

public class ValidateConfigEntries {

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
        }
    }
}
