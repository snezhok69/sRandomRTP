package org.sRandomRTP.Services;

import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.sRandomRTP.Utils.PlayerResourceMap;

/**
 * Per-player countdown boss-bar state.
 */
public final class PlayerBarStateStore {

    private final PlayerResourceMap<BossBar> bossBars = new PlayerResourceMap<>();

    public PlayerResourceMap<BossBar> getBossBars() { return bossBars; }

    /** Cancels and removes all bar-related state for the given player. */
    public void clearPlayer(Player player) {
        if (player == null) return;
        bossBars.remove(player);
    }
}
