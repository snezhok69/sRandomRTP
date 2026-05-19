package org.sRandomRTP.DifferentMethods.BossBars;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

public class RemoveAllBossBars {

    public static void removeBossBar(Player player) {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            BossBar bossBar = state.getBossBars().remove(player);
            if (bossBar != null) {
                bossBar.removeAll();
            }
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(RemoveAllBossBars.class, e);
        }
    }

    public static void removeAllBossBars() {
        try {
            RuntimeStateRegistry state = Variables.getRuntimeState();
            for (BossBar bossBar : state.getBossBars().values()) {
                bossBar.removeAll();
            }
            state.getBossBars().clear();
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(RemoveAllBossBars.class, e);
        }
    }
}
