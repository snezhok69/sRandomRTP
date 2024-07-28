package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.sRandomRTP.DifferentMethods.Variables;

import java.lang.reflect.Method;

public class CheckingServerVersion {

    public static boolean checkingServerVersion() {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] packageParts = packageName.split("\\.");
            if (packageParts.length >= 4) {
                String version = packageParts[3];
                String minorVersionStr = version.split("_")[1];
                int minorVersion = Integer.parseInt(minorVersionStr);
                if (minorVersion < 9) {
                    Variables.pluginToggle = true;
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cYour server version is lower than §61.9 §cPlugin has been disabled!");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cIt is recommended to update to version §61.9 §cor higher");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §cfor full compatibility.");
                    Bukkit.getConsoleSender().sendMessage("");
                    Bukkit.getConsoleSender().sendMessage(Variables.pluginName + " §8- §c>==========================================<");
                    Bukkit.getConsoleSender().sendMessage("");
                    Class<?> bukkitClass = Class.forName("org.bukkit.Bukkit");
                    Method getServerMethod = bukkitClass.getMethod("getServer");
                    Object serverObject = getServerMethod.invoke(null);
                    Class<?> serverClass = serverObject.getClass();
                    Method getPluginManagerMethod = serverClass.getMethod("getPluginManager");
                    Object pluginManagerObject = getPluginManagerMethod.invoke(serverObject);
                    Class<?> pluginManagerClass = pluginManagerObject.getClass();
                    Method disablePluginMethod = pluginManagerClass.getMethod("disablePlugin", Plugin.class);
                    disablePluginMethod.invoke(pluginManagerObject, Variables.getInstance());
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
        }
        return false;
    }
}