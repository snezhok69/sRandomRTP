package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.logging.Level;

public class CheckingServerVersion {

    public static boolean checkingServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] packageParts = packageName.split("\\.");
            if (packageParts.length >= 4) {
                String version = packageParts[3];
                String minorVersionStr = version.split("_")[1];
                int minorVersion = Integer.parseInt(minorVersionStr);
                if (minorVersion < 16) {
                    Variables.pluginToggle = true;
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cYour server version is lower than §61.16 §cPlugin has been disabled!");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cIt is recommended to update to version §61.16 §cor higher");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cfor full compatibility.");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getPluginManager().disablePlugin(Variables.getInstance());
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            Variables.getInstance().getLogger().log(Level.SEVERE, "Failed to verify server version", e);
        }
        return false;
    }
}