package org.sRandomRTP.Cooldowns;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Data.DataSave;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpWorld;
import static org.sRandomRTP.DifferentMethods.Variables.teleportTasks;

public class CooldownBypassBossBarWorld {
    public static boolean cooldownBypassBossBarworld(Player player, CommandSender sender, String worldName) {
        try {
            World targetWorld = Bukkit.getWorld(worldName);
            if (player.hasPermission("sRandomRTP.Cooldown.bypass")) {
                RtpRtpWorld.rtpRtpworld(sender, player, targetWorld);
            } else {
                startBossBarCountdown(player, sender, worldName);
                return true;
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }

    public static void startBossBarCountdown(Player player, CommandSender sender, String worldName) {
        int countdownTime = Variables.bossbarfile.getInt("teleport.bossbar-time");
        final double[] timeLeft = {countdownTime};
        final long[] lastSoundTime = {System.currentTimeMillis()};

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

        WrappedTask progressTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            timeLeft[0] -= 0.06;

            long currentTime = System.currentTimeMillis();
            World targetWorld = Bukkit.getWorld(worldName);
            if (currentTime - lastSoundTime[0] >= 1000) {
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
                lastSoundTime[0] = currentTime;
            }

            if (timeLeft[0] <= 0) {
                WrappedTask task = teleportTasks.get(player);
                if (task != null) {
                    task.cancel();
                    teleportTasks.remove(player);
                }
                RemoveBossBar.removeBossBar(player);
                Variables.playerSearchStatus.put(player.getName(), false);
                RtpRtpWorld.rtpRtpworld(sender, player, targetWorld);
                int totalUses = Variables.rtpCount.getOrDefault(1, 0);
                Variables.rtpCount.put(1, totalUses + 1);
                DataSave.dataSave();
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

        Variables.teleportTasks.put(player.getPlayer(), progressTask);
        Variables.playerSearchStatus.put(player.getName(), true);
    }
}