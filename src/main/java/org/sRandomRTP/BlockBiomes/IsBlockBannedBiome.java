package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

public class IsBlockBannedBiome {
    public static boolean isBlockBannedbiome(Material material) {
        try {
            return Variables.blockListbiome.contains(material);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}