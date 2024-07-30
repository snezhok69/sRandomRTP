package org.sRandomRTP.DifferentMethods;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetBossBarProgress {
    public static void setBossBarProgress(CommandSender sender, Player player, double progress, String message) {
        try {
                BossBar existingBossBar = Variables.bossBars.get(player);
                if (existingBossBar == null) {
                    BarColor color = BarColor.valueOf(Variables.bossbarfile.getString("teleport.bar-color"));
                    BarStyle style = BarStyle.valueOf(Variables.bossbarfile.getString("teleport.bar-style"));
                    BossBar bossBar = Bukkit.createBossBar(message, color, style);
                    bossBar.setProgress(1.0);
                    bossBar.addPlayer(player);
                    Variables.bossBars.put(player, bossBar);
                } else {
                    existingBossBar.setProgress(progress);
                    existingBossBar.setTitle(message);
                }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}