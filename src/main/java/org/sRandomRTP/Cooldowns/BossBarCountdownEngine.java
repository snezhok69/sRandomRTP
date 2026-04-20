package org.sRandomRTP.Cooldowns;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import java.util.concurrent.atomic.AtomicReference;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.Permissions;
import org.sRandomRTP.Data.RtpCountDataStore;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.BossBars.RemoveAllBossBars;
import org.sRandomRTP.DifferentMethods.BossBars.SetBossBarProgress;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.RuntimeStateRegistry;


public final class BossBarCountdownEngine {

    /** Seconds decremented from the countdown per timer tick (1 tick ≈ 50 ms, tuned to ~0.06 s). */
    private static final double TICK_DECREMENT_PER_PERIOD = 0.06;
    /** Minimum interval between sound plays in milliseconds. */
    private static final long   SOUND_INTERVAL_MS         = 1000L;
    /** Initial delay before first timer execution (ticks). */
    private static final long   TASK_DELAY_TICKS          = 1L;
    /** Period between timer executions (ticks). */
    private static final long   TASK_PERIOD_TICKS         = 1L;

    private BossBarCountdownEngine() {}

    /**
     * Runs action immediately for players with cooldown bypass permission,
     * otherwise starts the shared boss-bar countdown.
     */
    public static void dispatch(Player player, CommandSender sender, Runnable action) {
        if (player.hasPermission(Permissions.COOLDOWN_BYPASS)) {
            action.run();
            return;
        }
        startCountdown(player, sender, action);
    }

    /**
     * Starts the boss bar countdown for a player.
     * @param player     the player receiving the countdown (bossbar, sounds, action bar)
     * @param sender     the command sender (used for bossbar progress updates)
     * @param onComplete called when the countdown reaches zero; contains the RTP dispatch call
     */
    public static void startCountdown(Player player, CommandSender sender, Runnable onComplete) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        // Read config once at countdown start, not on every tick
        final int countdownTime = Variables.configCache.bossBarTime;
        final double[] timeLeft = {countdownTime};
        final long[] lastSoundTime = {System.currentTimeMillis()};
        // AtomicReference allows the task to safely cancel itself from within its own body,
        // even on Folia where the scheduler may invoke the lambda before returning the task handle.
        final AtomicReference<WrappedTask> progressTaskRef = new AtomicReference<>();

        playSound(player);

        WrappedTask task = Variables.getFoliaLib().getImpl().runAtEntityTimer(player, () -> {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null) {
                WrappedTask self = progressTaskRef.get();
                if (self != null) self.cancel();
                state.removeTeleportTask(player);
                return;
            }

            timeLeft[0] -= TICK_DECREMENT_PER_PERIOD;

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSoundTime[0] >= SOUND_INTERVAL_MS) {
                playSound(onlinePlayer);
                lastSoundTime[0] = currentTime;
            }

            if (timeLeft[0] <= 0) {
                WrappedTask self = progressTaskRef.get();
                if (self != null) self.cancel();
                state.removeTeleportTask(player);
                RemoveAllBossBars.removeBossBar(player);
                state.setPlayerSearching(player, false);
                onComplete.run();
                RuntimeStateRegistry freshState = Variables.getRuntimeState();
                if (freshState != null) freshState.getRtpCount().incrementAndGet();
                RtpCountDataStore.save();
            } else {
                updateBossBarAndActionBar(sender, player, onlinePlayer, timeLeft[0], countdownTime);
            }
        }, TASK_DELAY_TICKS, TASK_PERIOD_TICKS);

        progressTaskRef.set(task);
        state.putTeleportTask(player, task);
        state.setPlayerSearching(player, true);
    }

    private static void playSound(Player player) {
        if (!Variables.configCache.bossBarSoundEnabled) return;
        try {
            Sound sound = Sound.valueOf(Variables.configCache.bossBarSoundName.toUpperCase());
            player.playSound(player.getLocation(), sound, Variables.configCache.bossBarSoundVolume, Variables.configCache.bossBarSoundPitch);
        } catch (IllegalArgumentException e) {
            Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + Variables.configCache.bossBarSoundName);
        }
    }

    private static void updateBossBarAndActionBar(CommandSender sender, Player player,
                                                   Player onlinePlayer, double timeLeft, int countdownTime) {
        double progress = timeLeft / countdownTime;
        String bossbarmessage = LoadMessages.bossbar;
        if (bossbarmessage != null && !bossbarmessage.isEmpty()) {
            bossbarmessage = bossbarmessage.replace("%time%", String.valueOf((int) Math.ceil(timeLeft)));
            bossbarmessage = TranslateRGBColors.translateRGBColors(
                    ChatColor.translateAlternateColorCodes('&', bossbarmessage));
            if (Variables.configCache.bossBarEnabled) {
                SetBossBarProgress.setBossBarProgress(sender, player, progress, bossbarmessage);
            }
        }
        if (Variables.configCache.actionBarEnabled
                && LoadMessages.actionBarMessage != null
                && !LoadMessages.actionBarMessage.isEmpty()) {
            String formattedLine = String.format(LoadMessages.actionBarMessage, (int) Math.ceil(timeLeft));
            formattedLine = TranslateRGBColors.translateRGBColors(
                    net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', formattedLine));
            onlinePlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(formattedLine));
        }
    }
}
