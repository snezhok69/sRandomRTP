package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.popcraft.chunky.api.ChunkyAPI;
import org.sRandomRTP.DifferentMethods.Variables;

public class CheckingInstalledChunky {
    public static boolean сheckingInstalledсhunky() {
        String version = Bukkit.getServer().getBukkitVersion();
        String[] versionParts = version.split("\\.");
        if (versionParts.length >= 2) {
            try {
                int majorVersion = Integer.parseInt(versionParts[0]); // 1
                int minorVersion = Integer.parseInt(versionParts[1].split("-")[0]); // 12, 13, 14 и т.д.

                if (majorVersion == 1 && minorVersion < 13) {
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eChunky API не поддерживается на версии " + version);
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §eНекоторые функции будут недоступны!");
                    return false;
                }
            } catch (NumberFormatException e) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при определении версии сервера!");
                return false;
            }
        }

        try {
            Variables.chunkyAPI = Bukkit.getServicesManager().load(ChunkyAPI.class);
            if (Variables.chunkyAPI == null) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cНе удалось загрузить Chunky API!");
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cНекоторые функции будут недоступны!");
                return false;
            }
            if (Bukkit.getPluginManager().getPlugin("Chunky") == null) {
                Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cНекоторые функции будут недоступны!");
                return false;
            }
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cОшибка при загрузке Chunky API!");
            Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cНекоторые функции будут недоступны!");
            return false;
        }
    }
}