package org.sRandomRTP.Events;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.RemoveBossBar;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import java.util.List;

public class PlayerMove implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        try {
            if (Variables.teleportfile.getBoolean("teleport.move-cancel-rtp")) {
                Player player = event.getPlayer();
                if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                    if (Variables.teleportTasks.containsKey(player)) {
                        WrappedTask[] tasks = Variables.teleportTasks.get(player);
                        for (WrappedTask tasks1 : tasks) {
                            tasks1.cancel();
                        }
                        Variables.teleportTasks.remove(player);
                        //
                        List<String> formattedMessage = LoadMessages.teleportmovecancel;
                        for (String line : formattedMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        if (!Variables.teleportfile.getBoolean("teleport.Cooldowns.move-cancel-cooldown")) {
                            Variables.cooldowns.remove(player.getName());
                        }
                        //
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
                        RemoveBossBar.removeBossBar(player);
                        Variables.playerSearchStatus.put(player.getName(), false);
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}
