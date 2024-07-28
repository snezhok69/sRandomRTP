package org.sRandomRTP.DifferentMethods;

import org.bukkit.entity.Player;

import java.util.List;

public class FindNearestPlayerNear {
    public static Player findNearestPlayer(Player player, List<Player> allPlayers) {
        try {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player p : allPlayers) {
            if (p != player) {
                double distance = player.getLocation().distanceSquared(p.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestPlayer = p;
                }
            }
        }
        return nearestPlayer;
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return player;
    }
}