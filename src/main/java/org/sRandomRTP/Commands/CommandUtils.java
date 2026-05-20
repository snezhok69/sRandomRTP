package org.sRandomRTP.Commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Utils.ChatUtils;

import java.util.Optional;

/**
 * Shared helpers for command boilerplate — player-only guard and permission check.
 */
public final class CommandUtils {

    private CommandUtils() {}

    /**
     * Ensures the sender is a Player. Sends the player-only message if not.
     *
     * @return the Player wrapped in Optional, or empty if sender is not a player
     */
    public static Optional<Player> requirePlayer(CommandSender sender) {
        if (sender instanceof Player) {
            return Optional.of((Player) sender);
        }
        ChatUtils.sendPlayersOnly(sender);
        return Optional.empty();
    }

    /**
     * Checks that the sender has the given permission.
     * Sends the no-permission message and returns {@code false} if the check fails.
     */
    public static boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        Variables.getMessageService().send(sender, LoadMessages.nopermissioncommand);
        return false;
    }

    /**
     * Guards commands that require WorldGuard when region-checking is enabled.
     * Sends an error message to the sender and returns {@code false} if WG is absent.
     */
    public static boolean checkWorldGuard(CommandSender sender, boolean loggingEnabled) {
        org.bukkit.configuration.file.FileConfiguration teleportfile = Variables.getPluginContext().getConfigRegistry().getTeleportFile();
        if (teleportfile != null
                && teleportfile.getBoolean("teleport.checking-in-regions")
                && !Variables.getPluginContext().isWorldGuardAvailable()) {
            if (loggingEnabled) {
                org.bukkit.Bukkit.getConsoleSender().sendMessage(
                        "Install the WorldGuard plugin or disable checking regions "
                        + "in the configuration (checkinginregions: false).");
            }
            sender.sendMessage(ChatColor.RED + "Check the console. If there is nothing in the console, "
                    + "enable diagnostics in the configuration (diagnostic: true) and try teleportation again.");
            return false;
        }
        return true;
    }
}
