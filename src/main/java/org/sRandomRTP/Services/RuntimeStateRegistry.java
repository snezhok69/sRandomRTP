package org.sRandomRTP.Services;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.DataPortals.PortalStateStore;
import org.sRandomRTP.Utils.PlayerResourceMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mutable runtime-only state registry.
 *
 * <p>This keeps player-scoped tasks, temporary RTP/session flags and portal editor state in one
 * place so newer code can depend on a typed state layer instead of directly mutating global
 * static fields. The historical {@code Variables} class can then expose this registry as a thin
 * compatibility facade while the real ownership lives here.
 */
public final class RuntimeStateRegistry {

    /**
     * Three-way search phase for a player.
     *
     * <ul>
     *   <li>{@link #IDLE} — not searching; no tasks scheduled.</li>
     *   <li>{@link #SEARCHING} — search is in progress; tasks are running.</li>
     *   <li>{@link #TASKS_CLEANED} — search was cancelled and tasks were cancelled,
     *       but the final {@code finalizeTeleportStatus} has not fired yet.
     *       A second {@code cleanupTasks} call must be ignored in this state.</li>
     * </ul>
     *
     * Replaces the previous two-field pattern of
     * {@code Map<UUID,Boolean> playerSearchStatus} +
     * {@code Set<UUID> tasksCleanedButStatusActive}.
     */
    public enum SearchPhase { IDLE, SEARCHING, TASKS_CLEANED }

    /** Boss-bar and admin-bar state, owned by AdminBarService. */
    private final PlayerBarStateStore playerBarState = new PlayerBarStateStore();
    private final PlayerResourceMap<WrappedTask> teleportTasks = new PlayerResourceMap<>();
    private final PlayerResourceMap<WrappedTask> particleTasks = new PlayerResourceMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> biomeCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, SearchPhase> playerSearchPhase = new ConcurrentHashMap<>();
    /** Tracks who issued /rtp player — populated by CommandArgs, read by the teleport result dispatcher. */
    private final Map<UUID, CommandSender> senderSendMessage = new ConcurrentHashMap<>();
    /** Tracks the original sender for admin cooldown-bypass boss-bar flows (CooldownBypassBossBarPlayer). */
    private final Map<UUID, CommandSender> commandSenderMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerConfirmStatus = new ConcurrentHashMap<>();
    private final PlayerResourceMap<Location> initialPositions = new PlayerResourceMap<>();
    private final Map<UUID, AtomicBoolean> suitableLocationFound = new ConcurrentHashMap<>();
    /** Coherent portal-state sub-store (portals + blocks + tasks). */
    private final PortalStateStore portalState = new PortalStateStore();
    private final Map<UUID, World> targetWorlds = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger rtpCount = new java.util.concurrent.atomic.AtomicInteger(0);

    /** Returns the bar state sub-store (boss bars + admin bars). */
    public PlayerBarStateStore getPlayerBarState() {
        return playerBarState;
    }

    // ── Bar delegate methods (kept for backward compatibility) ───────────────
    public PlayerResourceMap<BossBar> getBossBars()                             { return playerBarState.getBossBars(); }
    public PlayerResourceMap<Map<AdminBarType, BossBar>> getAdminBossBars()     { return playerBarState.getAdminBossBars(); }
    public PlayerResourceMap<WrappedTask> getAdminBarTasks()                    { return playerBarState.getAdminBarTasks(); }
    public PlayerResourceMap<Set<AdminBarType>> getAdminBarTypes()              { return playerBarState.getAdminBarTypes(); }

    public PlayerResourceMap<WrappedTask> getTeleportTasks()  { return teleportTasks; }
    public PlayerResourceMap<WrappedTask> getParticleTasks()  { return particleTasks; }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    public Map<UUID, Long> getBiomeCooldowns() {
        return biomeCooldowns;
    }

    /** Returns the search-phase map for logging/diagnostics. */
    public Map<UUID, SearchPhase> getPlayerSearchStatus() {
        return playerSearchPhase;
    }

    public Map<UUID, CommandSender> getSenderSendMessage() {
        return senderSendMessage;
    }

    public Map<UUID, CommandSender> getCommandSenderMap() {
        return commandSenderMap;
    }

    public Map<UUID, Boolean> getPlayerConfirmStatus() {
        return playerConfirmStatus;
    }

    public PlayerResourceMap<Location> getInitialPositions() {
        return initialPositions;
    }

    public Map<UUID, AtomicBoolean> getSuitableLocationFound() {
        return suitableLocationFound;
    }

    /** Returns the portal state sub-store (portals, blocks, tasks). */
    public PortalStateStore getPortalState() {
        return portalState;
    }

    // ── Portal delegate methods (kept for backward compatibility) ────────────

    public Map<Location, Material> getPlacedBlocks()                          { return portalState.getPlacedBlocks(); }
    public Map<String, Map<String, PortalData>> getPlayerPortals()            { return portalState.getAllPlayerPortals(); }
    public Map<String, PortalData> getPlayerPortals(String playerName)        { return portalState.getPlayerPortals(playerName); }
    public Map<String, PortalData> ensurePlayerPortals(String playerName)     { return portalState.ensurePlayerPortals(playerName); }
    public PortalData getPlayerPortal(String playerName, String portalName)   { return portalState.getPlayerPortal(playerName, portalName); }
    public boolean hasPlayerPortal(String playerName, String portalName)      { return portalState.hasPlayerPortal(playerName, portalName); }
    public void putPlayerPortal(String p, String n, PortalData d)             { portalState.putPlayerPortal(p, n, d); }
    public PortalData removePlayerPortal(String playerName, String portalName){ return portalState.removePlayerPortal(playerName, portalName); }
    public List<String> getMatchingPortalNames(String p, String prefix, int l){ return portalState.getMatchingPortalNames(p, prefix, l); }
    public Map<String, PortalDataBlocks> getPlayerPortalsBlocks()             { return portalState.getPlayerPortalsBlocks(); }
    public void putPortalBlock(String key, PortalDataBlocks bd)               { portalState.putPortalBlock(key, bd); }
    public boolean isPortalBlockAtLocation(String w, int x, int y, int z)    { return portalState.isPortalBlockAtLocation(w, x, y, z); }
    public void clearPortalBlocks()                                           { portalState.clearPortalBlocks(); }
    public List<PortalDataBlocks> removePortalBlocksForPortal(String name)    { return portalState.removePortalBlocksForPortal(name); }
    public Map<String, PortalDataTasks> getPlayerPortalsTasks()               { return portalState.getPlayerPortalsTasks(); }
    public PortalDataTasks getPortalTask(String portalName)                   { return portalState.getPortalTask(portalName); }
    public void putPortalTask(String portalName, PortalDataTasks taskData)    { portalState.putPortalTask(portalName, taskData); }
    public PortalDataTasks removePortalTask(String portalName)                { return portalState.removePortalTask(portalName); }

    public Map<UUID, World> getTargetWorlds() {
        return targetWorlds;
    }

    public java.util.concurrent.atomic.AtomicInteger getRtpCount() {
        return rtpCount;
    }

    /** Returns {@code true} if the player is in the {@link SearchPhase#SEARCHING} or
     *  {@link SearchPhase#TASKS_CLEANED} phase (i.e., a search is active or was just cleaned). */
    public boolean isPlayerSearching(Player player) {
        if (player == null) return false;
        SearchPhase phase = playerSearchPhase.get(player.getUniqueId());
        return phase == SearchPhase.SEARCHING || phase == SearchPhase.TASKS_CLEANED;
    }

    public void setPlayerSearching(Player player, boolean searching) {
        if (player != null) {
            if (searching) {
                playerSearchPhase.put(player.getUniqueId(), SearchPhase.SEARCHING);
            } else {
                playerSearchPhase.remove(player.getUniqueId());
            }
        }
    }

    /** Sets the search phase directly; used by {@link org.sRandomRTP.DifferentMethods.Teleport.CleanupTasks}. */
    public void setSearchPhase(Player player, SearchPhase phase) {
        if (player != null) {
            if (phase == SearchPhase.IDLE) {
                playerSearchPhase.remove(player.getUniqueId());
            } else {
                playerSearchPhase.put(player.getUniqueId(), phase);
            }
        }
    }

    /** Returns the current search phase (never null; defaults to {@link SearchPhase#IDLE}). */
    public SearchPhase getSearchPhase(Player player) {
        if (player == null) return SearchPhase.IDLE;
        return playerSearchPhase.getOrDefault(player.getUniqueId(), SearchPhase.IDLE);
    }

    public void rememberInitialPosition(Player player) {
        if (player != null) {
            initialPositions.put(player, player.getLocation());
        }
    }

    public Location getInitialPosition(Player player) {
        return player == null ? null : initialPositions.get(player);
    }

    public Location removeInitialPosition(Player player) {
        return player == null ? null : initialPositions.remove(player);
    }

    public boolean hasTeleportTask(Player player) {
        return player != null && teleportTasks.containsKey(player);
    }

    public WrappedTask getTeleportTask(Player player) {
        return player == null ? null : teleportTasks.get(player);
    }

    public WrappedTask getTeleportTask(UUID playerId) {
        return playerId == null ? null : teleportTasks.get(playerId);
    }

    public void putTeleportTask(Player player, WrappedTask task) {
        if (player != null && task != null) {
            teleportTasks.put(player, task);
        }
    }

    public void putTeleportTask(UUID playerId, WrappedTask task) {
        if (playerId != null && task != null) {
            teleportTasks.put(playerId, task);
        }
    }

    public WrappedTask removeTeleportTask(Player player) {
        return player == null ? null : teleportTasks.remove(player);
    }

    public WrappedTask removeTeleportTask(UUID playerId) {
        return playerId == null ? null : teleportTasks.remove(playerId);
    }

    public WrappedTask getParticleTask(Player player) {
        return player == null ? null : particleTasks.get(player);
    }

    public void putParticleTask(Player player, WrappedTask task) {
        if (player != null && task != null) {
            particleTasks.put(player, task);
        }
    }

    public WrappedTask removeParticleTask(Player player) {
        return player == null ? null : particleTasks.remove(player);
    }

    public void clearTeleportFlags(Player player) {
        if (player != null) {
            suitableLocationFound.remove(player.getUniqueId());
        }
    }

    public void cleanExpiredCooldowns(long maxCooldownMs) {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> now - e.getValue() > maxCooldownMs);
        biomeCooldowns.entrySet().removeIf(e -> now - e.getValue() > maxCooldownMs);
    }

    public void clearPendingPlayerRouting(UUID playerId) {
        if (playerId == null) {
            return;
        }
        playerConfirmStatus.remove(playerId);
        senderSendMessage.remove(playerId);
        commandSenderMap.remove(playerId);
        targetWorlds.remove(playerId);
    }

    /**
     * Clears all runtime state for a player (disconnect, /rtp cancel, etc.).
     *
     * <p>The operation is intentionally non-atomic: each ConcurrentHashMap entry is removed
     * independently. A brief window of partial visibility between removals is acceptable —
     * cleanup only happens on player quit or explicit cancel, where racing on stale entries
     * is harmless.
     */
    public void clearPlayerRuntimeState(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        clearPendingPlayerRouting(playerId);
        cooldowns.remove(playerId);
        biomeCooldowns.remove(playerId);
        playerSearchPhase.remove(playerId);
        suitableLocationFound.remove(playerId);
        initialPositions.remove(player);
        teleportTasks.remove(player);
        particleTasks.remove(player);
        playerBarState.clearPlayer(player);
        org.sRandomRTP.Cooldowns.CooldownManager.instance().invalidatePermissionCache(playerId);
    }

    public Set<AdminBarType> getActiveAdminBarTypes(Player player) {
        Set<AdminBarType> activeTypes = player == null ? null : playerBarState.getAdminBarTypes().get(player);
        return activeTypes == null ? Collections.<AdminBarType>emptySet() : activeTypes;
    }
}
