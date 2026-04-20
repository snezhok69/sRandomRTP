package org.sRandomRTP.Utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

/**
 * Static chat/console logging helpers shared across the plugin.
 *
 * <p>All methods prefix messages with the standard plugin tag
 * ({@code §a[sRandomRTP] §8-}) so callers never need to repeat it.</p>
 */
public final class ChatUtils {

    /** Standard plugin name prefix applied to every message. */
    public static final String PLUGIN_NAME = "§a[sRandomRTP]";

    private ChatUtils() {}

    /** Sends a formatted error message: "[sRandomRTP] §8- §c{msg}". */
    public static void sendError(CommandSender sender, String msg) {
        sender.sendMessage(PLUGIN_NAME + " §8- §c" + msg);
    }

    /** Sends a formatted success message: "[sRandomRTP] §8- §a{msg}". */
    public static void sendSuccess(CommandSender sender, String msg) {
        sender.sendMessage(PLUGIN_NAME + " §8- §a" + msg);
    }

    /** Sends the standard "players only" message to a console/non-player sender. */
    public static void sendPlayersOnly(CommandSender sender) {
        sender.sendMessage(PLUGIN_NAME + " §8- §cOnly players can use this command.");
    }

    /** Logs an info message to the console: "[sRandomRTP] §8- §e{msg}". */
    public static void logInfo(String msg) {
        Bukkit.getConsoleSender().sendMessage(PLUGIN_NAME + " §8- §e" + msg);
    }

    /** Logs an error message to the console: "[sRandomRTP] §8- §c{msg}". */
    public static void logError(String msg) {
        Bukkit.getConsoleSender().sendMessage(PLUGIN_NAME + " §8- §c" + msg);
    }

    /** Logs a success message to the console: "[sRandomRTP] §8- §a{msg}". */
    public static void logSuccess(String msg) {
        Bukkit.getConsoleSender().sendMessage(PLUGIN_NAME + " §8- §a" + msg);
    }
}
