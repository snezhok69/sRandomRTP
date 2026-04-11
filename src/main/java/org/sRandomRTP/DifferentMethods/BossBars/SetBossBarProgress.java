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
            BossBar existingBossBar = state.getBossBars().get(player);
            if (existingBossBar == null) {
                BarColor color = BarColor.valueOf(Variables.bossbarfile.getString("teleport.bar-color"));
                BarStyle style = BarStyle.valueOf(Variables.bossbarfile.getString("teleport.bar-style"));
                BossBar bossBar = Bukkit.createBossBar(message, color, style);
                bossBar.setProgress(1.0);
                bossBar.addPlayer(player);
                state.getBossBars().put(player, bossBar);
            } else {
                existingBossBar.setProgress(progress);
                existingBossBar.setTitle(message);
            }
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(SetBossBarProgress.class, e);
        }
    }
}
