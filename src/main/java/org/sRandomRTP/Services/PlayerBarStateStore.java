package org.sRandomRTP.Services;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.sRandomRTP.Utils.PlayerResourceMap;

import java.util.Map;
import java.util.Set;

/**
 * Per-player boss-bar and admin-bar state.
 *
 * <p>Pulled out of {@link RuntimeStateRegistry} so that {@link AdminBarService}
 * owns the maps it exclusively reads and writes — no other component touches them.
 */
public final class PlayerBarStateStore {

    private final PlayerResourceMap<BossBar> bossBars = new PlayerResourceMap<>();
    private final PlayerResourceMap<Map<AdminBarType, BossBar>> adminBossBars = new PlayerResourceMap<>();
    private final PlayerResourceMap<WrappedTask> adminBarTasks = new PlayerResourceMap<>();
    private final PlayerResourceMap<Set<AdminBarType>> adminBarTypes = new PlayerResourceMap<>();

    public PlayerResourceMap<BossBar> getBossBars()                             { return bossBars; }
    public PlayerResourceMap<Map<AdminBarType, BossBar>> getAdminBossBars()     { return adminBossBars; }
    public PlayerResourceMap<WrappedTask> getAdminBarTasks()                    { return adminBarTasks; }
    public PlayerResourceMap<Set<AdminBarType>> getAdminBarTypes()              { return adminBarTypes; }

    /** Cancels and removes all bar-related state for the given player. */
    public void clearPlayer(Player player) {
        if (player == null) return;
        WrappedTask adminTask = adminBarTasks.remove(player);
        if (adminTask != null) adminTask.cancel();
        adminBarTypes.remove(player);
        adminBossBars.remove(player);
    }
}
