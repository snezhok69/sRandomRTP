package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.popcraft.chunky.api.ChunkyAPI;
import org.sRandomRTP.DifferentMethods.Variables;

public class CheckingInstalledChunky {
    public static boolean checkingInstalledChunky() {
        String version = Bukkit.getServer().getBukkitVersion();
        String[] versionParts = version.split("\\.");
        if (versionParts.length >= 2) {
            try {
                int majorVersion = Integer.parseInt(versionParts[0]);
                int minorVersion = Integer.parseInt(versionParts[1].split("-")[0]);

                if (majorVersion == 1 && minorVersion < 13) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChunky API is not supported on version " + version);
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eSome functions will not be available!");
                    return false;
                }
            } catch (NumberFormatException e) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cError in determining the server version!");
                return false;
            }
        }

        try {
            Variables.chunkyAPI = Bukkit.getServicesManager().load(ChunkyAPI.class);
            if (Variables.chunkyAPI == null) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cFailed to load Chunky API!");
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cSome functions will not be available!");
                return false;
            }
            if (Bukkit.getPluginManager().getPlugin("Chunky") == null) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cSome functions will not be available!");
                return false;
            }
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cError loading Chunky API!");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cSome functions will not be available!");
            return false;
        }
    }
}