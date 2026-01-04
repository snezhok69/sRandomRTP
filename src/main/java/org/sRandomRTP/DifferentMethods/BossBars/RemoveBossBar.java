package org.sRandomRTP.DifferentMethods.BossBars;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

public class RemoveBossBar {
    public static void removeBossBar(Player player) {
        try {
            BossBar bossBar = Variables.bossBars.remove(player);
            if (bossBar != null) {
                bossBar.removeAll();
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}