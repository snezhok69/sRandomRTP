package org.sRandomRTP.Cooldowns;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Data.DataSave;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpBase;

public class CooldownBypassBossBarBase {

    public static boolean cooldownBypassBossBarbase(Player player, CommandSender sender) {
        try {
            if (player.hasPermission("sRandomRTP.Cooldown.bypass")) {
                RtpRtpBase.rtpRtpbase(sender);
            } else {
                startBossBarCountdown(player, sender);
                return true;
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }

    public static void startBossBarCountdown(Player player, CommandSender sender) {
        int countdownTime = Variables.bossbarfile.getInt("teleport.bossbar-time");
        WrappedTask[] tasks = new WrappedTask[2];
        final double[] timeLeft = {countdownTime};
        WrappedTask progressTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            timeLeft[0] -= 0.06;
            if (timeLeft[0] <= 0) {
                if (Variables.teleportTasks.containsKey(player)) {
                    WrappedTask[] tasks2 = Variables.teleportTasks.get(player);
                    for (WrappedTask tasks1 : tasks2) {
                        tasks1.cancel();
                    }
                    Variables.teleportTasks.remove(player);
                }
                RemoveBossBar.removeBossBar(player);
                Variables.playerSearchStatus.put(player.getName(), false);
                RtpRtpBase.rtpRtpbase(sender);
                int totalUses = Variables.rtpCount.getOrDefault(1, 0);
                Variables.rtpCount.put(1, totalUses + 1);
                DataSave.dataSave();
                tasks[1].cancel();
            } else {
                double progress = timeLeft[0] / countdownTime;
                String bossbarmessage = LoadMessages.bossbar;
                bossbarmessage = bossbarmessage.replace("%time%", String.valueOf((int) Math.ceil(timeLeft[0])));
                bossbarmessage = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', bossbarmessage));
                if (Variables.bossbarfile.getBoolean("teleport.bossbarEnabled")) {
                    SetBossBarProgress.setBossBarProgress(sender, player, progress, bossbarmessage);
                }
                if (Variables.bossbarfile.getBoolean("teleport.actionBarEnabled")) {
                    String formattedLine = String.format(LoadMessages.actionBarMessage, (int) Math.ceil(timeLeft[0]));
                    formattedLine = TranslateRGBColors.translateRGBColors(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', formattedLine));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedLine));
                }
            }
        }, 0, 1);
        WrappedTask soundtask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            if (Variables.soundfile.getBoolean("teleport.boss-bar-teleport-sound.enabled")) {
                String soundName = Variables.soundfile.getString("teleport.boss-bar-teleport-sound.sound");
                float volume = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.volume");
                float pitch = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.pitch");

                try {
                    Sound sound = Sound.valueOf(soundName.toUpperCase());
                    player.getPlayer().playSound(player.getPlayer().getLocation(), sound, volume, pitch);
                } catch (IllegalArgumentException e) {
                    Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + soundName);
                }
            }
        }, 0L, 20L);

        tasks[0] = progressTask;
        tasks[1] = soundtask;

        Variables.teleportTasks.put(player.getPlayer(), tasks);
        Variables.playerSearchStatus.put(player.getName(), true);
    }
}
