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
import org.sRandomRTP.Utils.PlayerResourceMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
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

    private final PlayerResourceMap<BossBar> bossBars = new PlayerResourceMap<>();
    private final PlayerResourceMap<Map<AdminBarType, BossBar>> adminBossBars = new PlayerResourceMap<>();
    private final PlayerResourceMap<WrappedTask> teleportTasks = new PlayerResourceMap<>();
    private final PlayerResourceMap<WrappedTask> adminBarTasks = new PlayerResourceMap<>();
    private final PlayerResourceMap<Set<AdminBarType>> adminBarTypes = new PlayerResourceMap<>();
    private final PlayerResourceMap<WrappedTask> particleTasks = new PlayerResourceMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> biomeCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Boolean> playerSearchStatus = new ConcurrentHashMap<>();
    private final Map<String, CommandSender> senderSendMessage = new ConcurrentHashMap<>();
    private final Map<String, CommandSender> commandSenderMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> playerConfirmStatus = new ConcurrentHashMap<>();
    private final PlayerResourceMap<Location> initialPositions = new PlayerResourceMap<>();
    private final Map<String, AtomicBoolean> suitableLocationFound = new ConcurrentHashMap<>();
    private final Map<Location, Material> placedBlocks = new ConcurrentHashMap<>();
    private final Map<String, Map<String, PortalData>> playerPortals = new ConcurrentHashMap<>();
    private final Map<String, PortalDataBlocks> playerPortalsBlocks = new ConcurrentHashMap<>();
    // Spatial index: "world,x,y,z" → true for O(1) portal-block lookups in block-break events
    private final Map<String, Boolean> portalBlockLocationIndex = new ConcurrentHashMap<>();
    // Reverse index: portalName → set of block keys, for O(1) portal removal
    private final Map<String, Set<String>> portalNameToBlockKeys = new ConcurrentHashMap<>();
    // Lock for compound operations that must update both maps atomically
    private final Object portalBlockLock = new Object();
    private final Map<String, PortalDataTasks> playerPortalsTasks = new ConcurrentHashMap<>();
    private final Map<String, World> targetWorlds = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger rtpCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public PlayerResourceMap<BossBar> getBossBars() {
        return bossBars;
    }

    public PlayerResourceMap<Map<AdminBarType, BossBar>> getAdminBossBars() {
        return adminBossBars;
    }

    public PlayerResourceMap<WrappedTask> getTeleportTasks() {
        return teleportTasks;
    }

    public PlayerResourceMap<WrappedTask> getAdminBarTasks() {
        return adminBarTasks;
    }

    public PlayerResourceMap<Set<AdminBarType>> getAdminBarTypes() {
        return adminBarTypes;
    }

    public PlayerResourceMap<WrappedTask> getParticleTasks() {
        return particleTasks;
    }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    public Map<UUID, Long> getBiomeCooldowns() {
        return biomeCooldowns;
    }

    public Map<String, Boolean> getPlayerSearchStatus() {
        return playerSearchStatus;
    }

    public Map<String, CommandSender> getSenderSendMessage() {
        return senderSendMessage;
    }

    public Map<String, CommandSender> getCommandSenderMap() {
        return commandSenderMap;
    }

    public Map<String, Boolean> getPlayerConfirmStatus() {
        return playerConfirmStatus;
    }

    public PlayerResourceMap<Location> getInitialPositions() {
        return initialPositions;
    }

    public Map<String, AtomicBoolean> getSuitableLocationFound() {
        return suitableLocationFound;
    }

    public Map<Location, Material> getPlacedBlocks() {
        return placedBlocks;
    }

    public Map<String, Map<String, PortalData>> getPlayerPortals() {
        return playerPortals;
    }

    public Map<String, PortalData> getPlayerPortals(String playerName) {
        if (playerName == null) {
            return Collections.<String, PortalData>emptyMap();
        }
        Map<String, PortalData> portals = playerPortals.get(playerName);
        return portals != null ? portals : Collections.<String, PortalData>emptyMap();
    }

    public Map<String, PortalData> ensurePlayerPortals(String playerName) {
        if (playerName == null) {
            return Collections.emptyMap();
        }
        return playerPortals.computeIfAbsent(playerName, key -> new ConcurrentHashMap<String, PortalData>());
    }

    public PortalData getPlayerPortal(String playerName, String portalName) {
        if (playerName == null || portalName == null) {
            return null;
        }
        return getPlayerPortals(playerName).get(portalName);
    }

    public boolean hasPlayerPortal(String playerName, String portalName) {
        return getPlayerPortal(playerName, portalName) != null;
    }

    public void putPlayerPortal(String playerName, String portalName, PortalData portalData) {
        if (playerName == null || portalName == null || portalData == null) {
            return;
        }
        ensurePlayerPortals(playerName).put(portalName, portalData);
    }

    public PortalData removePlayerPortal(String playerName, String portalName) {
        if (playerName == null || portalName == null) {
            return null;
        }
        final PortalData[] holder = {null};
        playerPortals.computeIfPresent(playerName, (k, portals) -> {
            holder[0] = portals.remove(portalName);
            return portals.isEmpty() ? null : portals;
        });
        return holder[0];
    }

    public List<String> getMatchingPortalNames(String playerName, String prefix, int limit) {
        List<String> names = new ArrayList<String>();
        if (playerName == null) {
            return names;
        }
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase();
        for (String portalName : getPlayerPortals(playerName).keySet()) {
            if (portalName != null && portalName.toLowerCase().startsWith(normalizedPrefix)) {
                names.add(portalName);
                if (limit > 0 && names.size() >= limit) {
                    break;
                }
            }
        }
        return names;
    }

    public Map<String, PortalDataBlocks> getPlayerPortalsBlocks() {
        return playerPortalsBlocks;
    }

    public void putPortalBlock(String key, PortalDataBlocks blockData) {
        if (key != null && blockData != null) {
            synchronized (portalBlockLock) {
                playerPortalsBlocks.put(key, blockData);
                String locKey = portalLocationKey(blockData.getWorld(), blockData.getX(), blockData.getY(), blockData.getZ());
                portalBlockLocationIndex.put(locKey, Boolean.TRUE);
                if (blockData.getPortalName() != null) {
                    portalNameToBlockKeys.computeIfAbsent(blockData.getPortalName(), k -> ConcurrentHashMap.newKeySet()).add(key);
                }
            }
        }
    }

    /** O(1) check — replaces the O(n) iteration over playerPortalsBlocks.values(). */
    public boolean isPortalBlockAtLocation(String world, int x, int y, int z) {
        return portalBlockLocationIndex.containsKey(portalLocationKey(world, x, y, z));
    }

    /** Clears both the block map and the spatial index together. */
    public void clearPortalBlocks() {
        synchronized (portalBlockLock) {
            playerPortalsBlocks.clear();
            portalBlockLocationIndex.clear();
            portalNameToBlockKeys.clear();
        }
    }

    /**
     * Removes all blocks belonging to {@code portalName} from both maps.
     * Returns the removed blocks so callers can perform world-side cleanup.
     */
    public List<PortalDataBlocks> removePortalBlocksForPortal(String portalName) {
        List<PortalDataBlocks> removed = new ArrayList<>();
        if (portalName == null) return removed;
        synchronized (portalBlockLock) {
            Set<String> keys = portalNameToBlockKeys.remove(portalName);
            if (keys == null) return removed;
            for (String key : keys) {
                PortalDataBlocks bd = playerPortalsBlocks.remove(key);
                if (bd != null) {
                    portalBlockLocationIndex.remove(portalLocationKey(bd.getWorld(), bd.getX(), bd.getY(), bd.getZ()));
                    removed.add(bd);
                }
            }
        }
        return removed;
    }

    private static String portalLocationKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    public Map<String, PortalDataTasks> getPlayerPortalsTasks() {
        return playerPortalsTasks;
    }

    public PortalDataTasks getPortalTask(String portalName) {
        return portalName == null ? null : playerPortalsTasks.get(portalName);
    }

    public void putPortalTask(String portalName, PortalDataTasks taskData) {
        if (portalName != null && taskData != null) {
            playerPortalsTasks.put(portalName, taskData);
        }
    }

    public PortalDataTasks removePortalTask(String portalName) {
        if (portalName == null) return null;
        PortalDataTasks tasks = playerPortalsTasks.remove(portalName);
        if (tasks != null) {
            // Cancel the running Bukkit tasks so they don't fire after the portal is deleted.
            WrappedTask particles = tasks.getParticlesTask();
            if (particles != null && !particles.isCancelled()) {
                try { particles.cancel(); } catch (RuntimeException ignored) {}
            }
            WrappedTask trigger = tasks.getTriggerTask();
            if (trigger != null && !trigger.isCancelled()) {
                try { trigger.cancel(); } catch (RuntimeException ignored) {}
            }
        }
        return tasks;
    }

    public Map<String, World> getTargetWorlds() {
        return targetWorlds;
    }

    public java.util.concurrent.atomic.AtomicInteger getRtpCount() {
        return rtpCount;
    }

    public boolean isPlayerSearching(Player player) {
        return player != null && isPlayerSearching(player.getName());
    }

    public boolean isPlayerSearching(String playerName) {
        return playerName != null && playerSearchStatus.getOrDefault(playerName, false);
    }

    public void setPlayerSearching(Player player, boolean searching) {
        if (player != null) {
            setPlayerSearching(player.getName(), searching);
        }
    }

    public void setPlayerSearching(String playerName, boolean searching) {
        if (playerName != null) {
            playerSearchStatus.put(playerName, searching);
        }
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
            suitableLocationFound.remove(player.getName());
        }
    }

    public void cleanExpiredCooldowns(long maxCooldownMs) {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> now - e.getValue() > maxCooldownMs);
        biomeCooldowns.entrySet().removeIf(e -> now - e.getValue() > maxCooldownMs);
    }

    public void clearPendingPlayerRouting(String playerName) {
        if (playerName == null) {
            return;
        }
        playerConfirmStatus.remove(playerName);
        senderSendMessage.remove(playerName);
        commandSenderMap.remove(playerName);
        targetWorlds.remove(playerName);
    }

    public void clearPlayerRuntimeState(Player player) {
        if (player == null) {
            return;
        }
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();
        clearPendingPlayerRouting(playerName);
        cooldowns.remove(playerId);
        biomeCooldowns.remove(playerId);
        playerSearchStatus.remove(playerName);
        suitableLocationFound.remove(playerName);
        initialPositions.remove(player);
        teleportTasks.remove(player);
        particleTasks.remove(player);
        WrappedTask adminTask = adminBarTasks.remove(player);
        if (adminTask != null) adminTask.cancel();
        adminBarTypes.remove(player);
        adminBossBars.remove(player);
        org.sRandomRTP.Cooldowns.CooldownManager.invalidateCache(playerId);
    }

    public Set<AdminBarType> getActiveAdminBarTypes(Player player) {
        Set<AdminBarType> activeTypes = player == null ? null : adminBarTypes.get(player);
        return activeTypes == null ? Collections.<AdminBarType>emptySet() : activeTypes;
    }
}
