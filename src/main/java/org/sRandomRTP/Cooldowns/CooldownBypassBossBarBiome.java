package org.sRandomRTP.Cooldowns;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.sRandomRTP.Data.DataSave;
import org.sRandomRTP.DifferentMethods.*;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpBiome;

public class CooldownBypassBossBarBiome {
    public static boolean cooldownBypassBossBarbiome(Player player, CommandSender sender, Biome biome) {
        try {
            if (player.hasPermission("sRandomRTP.Cooldown.bypass")) {
                RtpRtpBiome.rtpRtpbiome(sender, biome);
            } else {
                startBossBarCountdown(player, sender, biome);
                return true;
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }

    public static void startBossBarCountdown(Player player, CommandSender sender, Biome biome) {
        int countdownTime = Variables.bossbarfile.getInt("teleport.bossbar-time");
        BukkitTask[] tasks = new BukkitTask[2];
        BukkitTask progressTask = new BukkitRunnable() {
            double timeLeft = countdownTime;

            @Override
            public void run() {
                timeLeft -= 0.06;
                if (timeLeft <= 0) {
                    cancel();
                    RemoveBossBar.removeBossBar(player);
                    Variables.playerSearchStatus.put(player.getName(), false);
                    RtpRtpBiome.rtpRtpbiome(sender, biome);
                    int totalUses = Variables.rtpCount.getOrDefault(1, 0);
                    Variables.rtpCount.put(1, totalUses + 1);
                    DataSave.dataSave();
                    tasks[1].cancel();
                } else {
                    double progress = timeLeft / countdownTime;
                    String bossbarmessage = LoadMessages.bossbar;
                    bossbarmessage = bossbarmessage.replace("%time%", String.valueOf((int) Math.ceil(timeLeft)));
                    bossbarmessage = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', bossbarmessage));
                    if (Variables.bossbarfile.getBoolean("teleport.bossbarEnabled")) {
                        SetBossBarProgress.setBossBarProgress(sender, player, progress, bossbarmessage);
                    }
                    if (Variables.bossbarfile.getBoolean("teleport.actionBarEnabled")) {
                        String formattedLine = String.format(LoadMessages.actionBarMessage, (int) Math.ceil(timeLeft));
                        formattedLine = TranslateRGBColors.translateRGBColors(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', formattedLine));
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedLine));
                    }
                }
            }
        }.runTaskTimerAsynchronously(Variables.getInstance(), 0, 1);
        BukkitTask soundTask = new BukkitRunnable() {
            @Override
            public void run() {
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
            }
        }.runTaskTimerAsynchronously(Variables.getInstance(), 0, 20);

        tasks[0] = progressTask;
        tasks[1] = soundTask;

        Variables.teleportTasks.put(player.getPlayer(), tasks);
        Variables.playerSearchStatus.put(player.getName(), true);
    }
}
