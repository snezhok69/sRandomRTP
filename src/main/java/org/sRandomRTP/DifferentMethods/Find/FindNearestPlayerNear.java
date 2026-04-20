package org.sRandomRTP.DifferentMethods.Find;

import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.ArrayList;
import java.util.List;

public class FindNearestPlayerNear {

    /**
     * Returns a random online player other than {@code player}, or {@code null} if
     * no other players are available. Named "Random" to reflect actual behaviour;
     * the previous "findNearestPlayer" wrapper was a misnomer and has been removed.
     */
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
