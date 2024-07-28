package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class IsOutdatedByMultipleVersionsTask {
    public static void isOutdatedByMultipleVersionsTask() {
        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    String currentVersion = Variables.getInstance().getDescription().getVersion();
                    String latestVersion = "2.9";
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
                }
            }.runTaskTimerAsynchronously(Variables.getInstance(), 0, 20);
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}