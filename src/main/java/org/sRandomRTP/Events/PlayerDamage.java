package org.sRandomRTP.Events;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveBossBar;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

import java.util.List;

public class PlayerDamage implements Listener {

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        try {
            if (!Variables.teleportfile.getBoolean("teleport.damaged-cancel-rtp")) {
                return;
            }
            if (!(event.getEntity() instanceof Player)) {
                return;
            }

            Player player = (Player) event.getEntity();
            boolean loggingEnabled = Variables.getInstance().getConfig().getBoolean("logs", false);
            TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
            boolean hasTask = Variables.teleportTasks.containsKey(player);

            if (context == null && !hasTask) {
                return;
            }

            if (context != null) {
                TeleportRequestManager.cancelRequest(player.getUniqueId(), loggingEnabled, "damage cancel");
            }
            if (hasTask) {
                WrappedTask checkProximityTaskTask = Variables.teleportTasks.remove(player);
                if (checkProximityTaskTask != null && !checkProximityTaskTask.isCancelled()) {
                    checkProximityTaskTask.cancel();
                }
            }

            List<String> formattedMessage = LoadMessages.teleportdamagedcancel;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                player.sendMessage(formattedLine);
            }

            if (!Variables.teleportfile.getBoolean("teleport.Cooldowns.dmg-cancel-cooldown")) {
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
