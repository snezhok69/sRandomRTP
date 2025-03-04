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
import org.sRandomRTP.Rtp.RtpRtpPlayer;
import java.util.List;
import static org.sRandomRTP.DifferentMethods.Variables.teleportTasks;

public class CooldownBypassBossBarPlayer {

    public static boolean cooldownBypassBossBarplayer(CommandSender sender, Player targetPlayer, World world) {
        try {
            if (targetPlayer.hasPermission("sRandomRTP.Cooldown.bypass")) {
                RtpRtpPlayer.rtprtpplayer(sender, targetPlayer, world);
            } else {
                if (Variables.playerConfirmStatus.getOrDefault(targetPlayer.getName(), false)) {
                    List<String> formattedMessage = LoadMessages.rtpplayeralreadyrequested;
                    for (String line : formattedMessage) {
                        String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                        sender.sendMessage(formattedLine);
                    }
                    return false;
                }

                Variables.playerConfirmStatus.put(targetPlayer.getName(), true);

                List<String> formattedMessage1 = LoadMessages.rtpplayerteleportrequestsent;
                for (String line : formattedMessage1) {
                    line = line.replace("%player%", targetPlayer.getName());
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    sender.sendMessage(formattedLine);
                }

                if (Variables.teleportfile.getBoolean("teleport.rtp-player-messages")) {
                    Variables.commandSenderMap.put(targetPlayer.getName(), sender);
                    sendInitialMessage(sender, targetPlayer);
                    return true;
                } else {
                    if (targetPlayer.hasPermission("sRandomRTP.Cooldown.bypass")) {
                        RtpRtpPlayer.rtprtpplayer(sender, targetPlayer, world);
                    } else {
                        CooldownBypassBossBarPlayer.startBossBarCountdown(sender, targetPlayer, world);
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return false;
    }

    private static void sendInitialMessage(CommandSender sender, Player targetPlayer) {
        List<String> formattedMessage = LoadMessages.rtpplayerrequestinitiator;
        for (String line : formattedMessage) {
            line = line.replace("%initiator%", sender.getName());
            String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
            targetPlayer.sendMessage(formattedLine);

            Variables.getFoliaLib().getImpl().runLaterAsync(() -> {
                if (Variables.playerConfirmStatus.getOrDefault(targetPlayer.getName(), false)) {
                    List<String> formattedMessage1 = LoadMessages.rtpplayertimeout;
                    for (String line1 : formattedMessage1) {
                        String formattedLine1 = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line1));
                        targetPlayer.sendMessage(formattedLine1);
                    }
                    Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
                } else {
                    Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
                }
            }, 200);
        }
    }

    public static void denyTeleport(CommandSender sender, Player targetPlayer) {
        String playerName = targetPlayer.getName();
        if (Variables.playerConfirmStatus.getOrDefault(playerName, false)) {
            Variables.playerConfirmStatus.put(playerName, false);
            List<String> formattedMessage = LoadMessages.rtpplayercanceled;
            for (String line : formattedMessage) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                targetPlayer.sendMessage(formattedLine);
            }
            CommandSender originalSender = Variables.commandSenderMap.get(playerName);
            if (originalSender != null) {
                List<String> formattedMessage1 = LoadMessages.rtpplayersendernotified;
                for (String line : formattedMessage1) {
                    line = line.replace("%target-player%", targetPlayer.getName());
                    String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                    originalSender.sendMessage(formattedLine);
                }
                Variables.commandSenderMap.remove(playerName);
            }
        } else {
            List<String> formattedMessage1 = LoadMessages.rtpplayernoactiveteleport;
            for (String line : formattedMessage1) {
                String formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', line));
                targetPlayer.sendMessage(formattedLine);
            }
        }
    }

    public static void startBossBarCountdown(CommandSender sender, Player targetPlayer, World world) {
        int countdownTime = Variables.bossbarfile.getInt("teleport.bossbar-time");
        WrappedTask[] tasks = new WrappedTask[2];
        final double[] timeLeft = {countdownTime};
        WrappedTask progressTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            timeLeft[0] -= 0.06;
            if (timeLeft[0] <= 0) {
                WrappedTask checkProximityTaskTask = teleportTasks.get(targetPlayer);
                if (checkProximityTaskTask != null) {
                    checkProximityTaskTask.cancel();
                    teleportTasks.remove(targetPlayer);
                }
                RemoveBossBar.removeBossBar(targetPlayer);
                Variables.playerSearchStatus.put(targetPlayer.getName(), false);
                RtpRtpPlayer.rtprtpplayer(sender, targetPlayer, world);
                Variables.playerConfirmStatus.put(targetPlayer.getName(), false);
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
                    SetBossBarProgress.setBossBarProgress(sender, targetPlayer, progress, bossbarmessage);
                }
                if (Variables.bossbarfile.getBoolean("teleport.actionBarEnabled")) {
                    String formattedLine = String.format(LoadMessages.actionBarMessage, (int) Math.ceil(timeLeft[0]));
                    formattedLine = TranslateRGBColors.translateRGBColors(net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', formattedLine));
                    targetPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedLine));
                }
            }
        }, 0, 1);
        WrappedTask soundTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
                if (Variables.soundfile.getBoolean("teleport.boss-bar-teleport-sound.enabled")) {
                    String soundName = Variables.soundfile.getString("teleport.boss-bar-teleport-sound.sound");
                    float volume = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.volume");
                    float pitch = (float) Variables.soundfile.getDouble("teleport.boss-bar-teleport-sound.pitch");

                    try {
                        Sound sound = Sound.valueOf(soundName.toUpperCase());
                        targetPlayer.getPlayer().playSound(targetPlayer.getPlayer().getLocation(), sound, volume, pitch);
                    } catch (IllegalArgumentException e) {
                        Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + soundName);
                    }
                }
        }, 0, 20);

        tasks[0] = progressTask;
        tasks[1] = soundTask;

        Variables.teleportTasks.put(targetPlayer.getPlayer(), tasks[0]);
        Variables.playerSearchStatus.put(targetPlayer.getName(), true);
    }
}
