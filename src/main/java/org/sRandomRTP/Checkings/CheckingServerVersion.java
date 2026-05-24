package org.sRandomRTP.Checkings;

import org.bukkit.Bukkit;
import org.sRandomRTP.Utils.ChatUtils;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.PluginVersionCatalog;
import org.sRandomRTP.Utils.ServerVersionParser;

import java.lang.reflect.Method;
import java.util.logging.Level;

public class CheckingServerVersion {

    public static boolean checkingServerVersion() {
        int minorVersion = resolveMinorVersion();
        if (minorVersion < 0) {
            return false; // could not determine version — allow plugin to start
        }
        if (minorVersion < PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR) {
            Variables.getPluginContext().setPluginToggle(true);
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cYour server version is lower than §61."
                    + PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR + " §cPlugin has been disabled!");
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cIt is recommended to update to version §61."
                    + PluginVersionCatalog.MIN_SUPPORTED_SERVER_MINOR + " §cor higher");
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §cfor full compatibility.");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getConsoleSender().sendMessage(ChatUtils.PLUGIN_NAME + " §8- §c>==========================================<");
            Bukkit.getConsoleSender().sendMessage("");
            Bukkit.getPluginManager().disablePlugin(Variables.getInstance());
            return true;
        }
        return false;
    }

    /**
     * Returns the Minecraft compatibility number (20 for 1.20.x, 26 for 26.1.x), or -1 on failure.
     *
     * <p>Uses {@link Bukkit#getMinecraftVersion()} as the primary strategy (stable on all
     * Paper-based forks including Purpur, Folia, and Paper 1.20.5+ which removed the versioned
     * NMS package). Falls back to parsing the CraftBukkit package name for very old servers.</p>
     */
    static int resolveMinorVersion() {
        // Primary: "1.20.4" -> 20, "26.1.1" -> 26.
        try {
            Method method = Bukkit.class.getMethod("getMinecraftVersion");
            Object value = method.invoke(null);
            String mc = value instanceof String ? (String) value : null;
            int parsed = ServerVersionParser.parseCompatibilityNumber(mc);
            if (parsed >= 0) {
                return parsed;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }

        // Fallback: "org.bukkit.craftbukkit.v1_20_R3" or "org.bukkit.craftbukkit.v26_1_R1".
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] packageParts = packageName.split("\\.");
            if (packageParts.length >= 4) {
                int parsed = ServerVersionParser.parseCraftBukkitPackageCompatibilityNumber(packageParts[3]);
                if (parsed >= 0) {
                    return parsed;
                }
            }
        } catch (RuntimeException ignored) {
        }

        Variables.getInstance().getLogger().log(Level.SEVERE, "Failed to determine server minor version");
        return -1;
    }
}
