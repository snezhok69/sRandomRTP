package org.sRandomRTP.DifferentMethods.rtprtps;

import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;

import java.util.UUID;

public class ComputeSessionSeed {
    public static long computeSessionSeed(Player player, TeleportRequestContext context) {
        UUID requestId = context.getRequestId();
        UUID playerId = player.getUniqueId();
        long seed = requestId.getMostSignificantBits() ^ requestId.getLeastSignificantBits();
        seed ^= playerId.getMostSignificantBits();
        seed ^= playerId.getLeastSignificantBits();
        seed ^= System.nanoTime();
        return seed;
    }
}
