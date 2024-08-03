package org.sRandomRTP.DifferentMethods;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class FindRandomPlayerNear {
    public static Player findRandomPlayer(Player player, List<Player> allPlayers) {
        try {
            List<Player> sameWorldPlayers = allPlayers.stream()
                    .filter(p -> p.getWorld().equals(player.getWorld()) && !p.equals(player))
                    .collect(Collectors.toList());
            if (sameWorldPlayers.isEmpty()) {
                return null;
            }
            Random random = new Random();
            return sameWorldPlayers.get(random.nextInt(sameWorldPlayers.size()));
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return null;
    }
}
