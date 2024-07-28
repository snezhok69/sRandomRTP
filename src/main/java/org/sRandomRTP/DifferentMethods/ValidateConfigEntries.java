package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.ArrayList;
import java.util.List;

public class ValidateConfigEntries {

    public static void validateConfigEntries(FileConfiguration config) {
        try {
        List<String> bannedBlocks = config.getStringList("teleport.bannedBlocks");
        List<String> invalidBlocks = new ArrayList<>();
        for (String block : bannedBlocks) {
            try {
                Material.valueOf(block.toUpperCase());
            } catch (IllegalArgumentException e) {
                invalidBlocks.add(block);
            }
        }
        if (!invalidBlocks.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("Invalid banned blocks: " + String.join(", ", invalidBlocks));
        }

        List<String> bannedBiomes = config.getStringList("teleport.bannedBiomes");
        List<String> invalidBiomes = new ArrayList<>();
        for (String biome : bannedBiomes) {
            try {
                Biome.valueOf(biome.toUpperCase());
            } catch (IllegalArgumentException e) {
                invalidBiomes.add(biome);
            }
        }
        if (!invalidBiomes.isEmpty()) {
            Bukkit.getConsoleSender().sendMessage("Invalid banned biomes: " + String.join(", ", invalidBiomes));
        }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}