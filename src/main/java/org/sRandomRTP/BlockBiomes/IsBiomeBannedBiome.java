package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class IsBiomeBannedBiome {
    public static boolean isBiomeBannedbiome(Biome biome) {
        try {
        FileConfiguration config = Variables.getInstance().getConfig();
        List<String> bannedBiomes = Variables.teleportfile.getStringList("teleport.bannedBiomescmdbiome");
        return bannedBiomes.contains(biome.name());
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}