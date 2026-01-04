package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveBossBar;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

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

            boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
            TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
            boolean hasTask = Variables.teleportTasks.containsKey(player);

            if (context == null && !hasTask) {
                List<String> formattedMessage = LoadMessages.nosearching;
                for (String line : formattedMessage) {
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }
                return;
            }

            if (context != null) {
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "cancel command");
            } else if (hasTask) {
                WrappedTask task = Variables.teleportTasks.remove(player);
                if (task != null && !task.isCancelled()) {
                    task.cancel();
                }
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
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}