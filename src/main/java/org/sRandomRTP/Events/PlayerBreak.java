package org.sRandomRTP.Events;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.RemoveBossBar;
import org.sRandomRTP.DifferentMethods.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

import static org.sRandomRTP.DifferentMethods.Variables.teleportTasks;

public class PlayerBreak implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    private void handleBlockEvent(Player player) {
        try {
            if (Variables.teleportfile.getBoolean("teleport.break-block-cancel-rtp")) {
                if (Variables.playerSearchStatus.get(player.getName()) != null && Variables.playerSearchStatus.get(player.getName())) {
                    if (teleportTasks.containsKey(player)) {
                        WrappedTask checkProximityTaskTask = teleportTasks.get(player);
                        if (checkProximityTaskTask != null) {
                            checkProximityTaskTask.cancel();
                            teleportTasks.remove(player);
                        }
                        List<String> formattedMessage = LoadMessages.teleportmovecancel;
                        for (String line : formattedMessage) {
                            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                            player.sendMessage(formattedLine);
                        }
                        if (!Variables.teleportfile.getBoolean("teleport.Cooldowns.break-block-cooldown")) {
                            Variables.cooldowns.remove(player.getName());
                        }
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
