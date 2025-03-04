package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.RemoveBossBar;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import java.util.List;

import static org.sRandomRTP.DifferentMethods.Variables.teleportTasks;

public class CommandCancel implements Listener {
    public static void commandRtpCancel(CommandSender sender) {
        try {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Variables.pluginName + " §cOnly players can execute this command!");
                return;
            }
            if (!sender.hasPermission("sRandomRTP.Command.Cancel")) {
                List<String> formattedMessage = LoadMessages.nopermissioncommand;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }
                return;
            }

            Player player = (Player) sender;

            if (Variables.teleportTasks.containsKey(player)) {
                WrappedTask task = Variables.teleportTasks.get(player);
                if (task != null) {
                    task.cancel();
                    Variables.teleportTasks.remove(player);
                }

                List<String> formattedMessage = LoadMessages.teleportcancel;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    player.sendMessage(formattedLine);
                }

                if (Variables.teleportfile.getBoolean("teleport.Cooldowns.dmg-cancel-cooldown")) {
                    Variables.cooldowns.remove(player.getName());
                }

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                RemoveBossBar.removeBossBar(player);
                Variables.playerSearchStatus.put(player.getName(), false);
            } else {
                List<String> formattedMessage = LoadMessages.nosearching;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}