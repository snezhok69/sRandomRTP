package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.PluginVersionCatalog;

import java.util.logging.Level;

public class CheckingServerVersion {

    public static boolean checkingServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] packageParts = packageName.split("\\.");
            if (packageParts.length >= 4) {
                String version = packageParts[3];
                String[] versionParts = version.split("_");
                if (versionParts.length < 2) return false;
                String minorVersionStr = versionParts[1];
                int minorVersion = Integer.parseInt(minorVersionStr);
                if (minorVersion < PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR) {
                    Variables.pluginToggle = true;
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cYour server version is lower than §61."
                            + PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR + " §cPlugin has been disabled!");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cIt is recommended to update to version §61."
                            + PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR + " §cor higher");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cfor full compatibility.");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getPluginManager().disablePlugin(Variables.getInstance());
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            Variables.getInstance().getLogger().log(Level.SEVERE, "Failed to verify server version", e);
        }
        return false;
    }
}
