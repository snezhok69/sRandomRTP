package org.sRandomRTP.DifferentMethods.BossBars;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class SetBossBarProgress {

    public static void setBossBarProgress(CommandSender sender, Player player, double progress, String message) {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            // computeIfAbsent is atomic on the ConcurrentHashMap-backed PlayerResourceMap,
            // preventing two concurrent RTP countdowns from creating duplicate bars for the same player.
            BossBar bar = state.getBossBars().computeIfAbsent(player, p -> {
                BarColor color = BarColor.valueOf(Variables.getPluginContext().getConfigRegistry().getBossBarFile().getString("teleport.bar-color"));
                BarStyle style = BarStyle.valueOf(Variables.getPluginContext().getConfigRegistry().getBossBarFile().getString("teleport.bar-style"));
                BossBar b = Bukkit.createBossBar(message, color, style);
                b.setProgress(1.0);
                b.addPlayer(p);
                return b;
            });
            bar.setProgress(progress);
            bar.setTitle(message);
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(SetBossBarProgress.class, e);
        }
    }
}
