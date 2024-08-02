package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;

public class IsOutdatedByMultipleVersionsTask {
    public static void isOutdatedByMultipleVersionsTask() {
        try {
            Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                String currentVersion = Variables.getInstance().getDescription().getVersion();
                String latestVersion = "3.0";
                String[] currentVersionParts = currentVersion.split("\\.");
                String[] latestVersionParts = latestVersion.split("\\.");
                int majorCurrentVersion = Integer.parseInt(currentVersionParts[0]);
                int minorCurrentVersion = Integer.parseInt(currentVersionParts[1]);
                int majorLatestVersion = Integer.parseInt(latestVersionParts[0]);
                int minorLatestVersion = Integer.parseInt(latestVersionParts[1]);
                int currentVersionInMinorVersions = (majorCurrentVersion * 10) + minorCurrentVersion;
                int latestVersionInMinorVersions = (majorLatestVersion * 10) + minorLatestVersion;
                int versionGap = latestVersionInMinorVersions - currentVersionInMinorVersions;
                if (versionGap >= 5) {
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c> WARNING ========================================== WARNING <");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cYour plugin is behind by §6" + versionGap + " §cversions! The latest version: §6" + latestVersion + "§c. Your version: §6" + currentVersion + "§c.");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cPlease update the plugin!");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c> WARNING ========================================== WARNING <");
                    Bukkit.getConsoleSender().sendMessage("");
                }
            }, 60 * 20, 60 * 20);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}