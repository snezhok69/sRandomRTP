package org.sRandomRTP.DifferentMethods.Find;

import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.ArrayList;
import java.util.List;

public class FindNearestPlayerNear {
    public static Player findNearestPlayer(Player player, List<Player> allPlayers) {
        return findRandomPlayer(player, allPlayers);
    }

    public static Player findRandomPlayer(Player player, List<Player> allPlayers) {
        List<Player> otherPlayers = new ArrayList<>();
        for (Player p : allPlayers) {
            if (p != player) {
                otherPlayers.add(p);
            }
        }

        if (otherPlayers.isEmpty()) {
            return null;
        }

        int randomIndex = Variables.getRngProvider().nextInt(otherPlayers.size());
        return otherPlayers.get(randomIndex);
    }
}
