package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

public class IsBlockBanned {
    public static boolean isBlockBanned(Material material) {
        try {
            return Variables.blockList.contains(material);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}