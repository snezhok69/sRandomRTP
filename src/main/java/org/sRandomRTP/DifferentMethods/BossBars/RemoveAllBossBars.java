package org.sRandomRTP.DifferentMethods.BossBars;

import org.bukkit.boss.BossBar;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

public class RemoveAllBossBars {
    public static void removeAllBossBars() {
        try {
            for (BossBar bossBar : Variables.bossBars.values()) {
                bossBar.removeAll();
            }
            Variables.bossBars.clear();
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
    }
}