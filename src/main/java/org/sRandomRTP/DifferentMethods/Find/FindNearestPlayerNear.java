package org.sRandomRTP.DifferentMethods.Find;

import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.LoggerUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FindNearestPlayerNear {
    public static Player findNearestPlayer(Player player, List<Player> allPlayers) {
        return findRandomPlayer(player, allPlayers);
    }

    public static Player findRandomPlayer(Player player, List<Player> allPlayers) {
        try {
            List<Player> otherPlayers = new ArrayList<>();
            for (Player p : allPlayers) {
                if (p != player) {
                    otherPlayers.add(p);
                }
            }

            if (otherPlayers.isEmpty()) {
                return null;
            }

            Random random = new Random();
            int randomIndex = random.nextInt(otherPlayers.size());
            return otherPlayers.get(randomIndex);
            
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return null;
    }
}