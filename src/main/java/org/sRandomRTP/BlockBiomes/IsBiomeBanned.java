package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import java.util.List;

public class IsBiomeBanned {
    public static boolean isBiomeBanned(Biome biome) {
        try {
            List<String> bannedBiomes = Variables.teleportfile.getStringList("teleport.bannedBiomes");
            return bannedBiomes.contains(biome.name());
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}