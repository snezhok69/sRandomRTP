package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Placeholders;
import org.sRandomRTP.DifferentMethods.Variables;

public class CheckingInstalledPlaceHolderAPI {

    public static boolean checkingInstalledPlaceHolderAPI() {
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new Placeholders().register();
            } else {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cPlaceHolderAPI is not installed, some features will be disabled!");
            }
            return false;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }
}