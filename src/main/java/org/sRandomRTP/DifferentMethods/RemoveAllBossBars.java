package org.sRandomRTP.DifferentMethods;

import org.bukkit.boss.BossBar;

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