package org.sRandomRTP.DataPortals;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates all in-memory portal editing state.
 *
 * <p>Pulled out of {@link org.sRandomRTP.Services.RuntimeStateRegistry} to give
 * the four portal maps (plus their lock) a cohesive home. All mutation of portal
 * spatial indices goes through this class to guarantee the compound invariant:
 * <em>playerPortalsBlocks, portalBlockLocationIndex, and portalNameToBlockKeys
 * are always consistent with each other.</em>
 */
public final class PortalStateStore {

    // Placed blocks during portal construction (temporary, cleared after commit)
    private final Map<Location, Material> placedBlocks = new ConcurrentHashMap<>();

    // Per-player portal registry
    private final Map<String, Map<String, PortalData>> playerPortals = new ConcurrentHashMap<>();
    private final Map<String, PortalDataBlocks> playerPortalsBlocks = new ConcurrentHashMap<>();

    // Spatial index: "world,x,y,z" → true for O(1) portal-block lookups in block-break events
    private final Map<String, Boolean> portalBlockLocationIndex = new ConcurrentHashMap<>();
    // Reverse index: portalName → set of block keys, for O(1) portal removal
    private final Map<String, Set<String>> portalNameToBlockKeys = new ConcurrentHashMap<>();
    // Lock for compound operations that must update multiple maps atomically
    private final Object portalBlockLock = new Object();

    // Running tasks associated with portals (particles, triggers)
    private final Map<String, PortalDataTasks> playerPortalsTasks = new ConcurrentHashMap<>();

    // ── Placed blocks ────────────────────────────────────────────────────────

    public Map<Location, Material> getPlacedBlocks() {
        return placedBlocks;
    }

    // ── Player portals ───────────────────────────────────────────────────────

    public Map<String, Map<String, PortalData>> getAllPlayerPortals() {
        return playerPortals;
    }

    public Map<String, PortalData> getPlayerPortals(String playerName) {
        if (playerName == null) return Collections.emptyMap();
        Map<String, PortalData> portals = playerPortals.get(playerName);
        return portals != null ? portals : Collections.emptyMap();
    }

    public Map<String, PortalData> ensurePlayerPortals(String playerName) {
        if (playerName == null) return Collections.emptyMap();
        return playerPortals.computeIfAbsent(playerName, k -> new ConcurrentHashMap<>());
    }

    public PortalData getPlayerPortal(String playerName, String portalName) {
        if (playerName == null || portalName == null) return null;
        return getPlayerPortals(playerName).get(portalName);
    }

    public boolean hasPlayerPortal(String playerName, String portalName) {
        return getPlayerPortal(playerName, portalName) != null;
    }

    public void putPlayerPortal(String playerName, String portalName, PortalData portalData) {
        if (playerName == null || portalName == null || portalData == null) return;
        ensurePlayerPortals(playerName).put(portalName, portalData);
    }

    public PortalData removePlayerPortal(String playerName, String portalName) {
        if (playerName == null || portalName == null) return null;
        final PortalData[] holder = {null};
        playerPortals.computeIfPresent(playerName, (k, portals) -> {
            holder[0] = portals.remove(portalName);
            return portals.isEmpty() ? null : portals;
        });
        return holder[0];
    }

    public List<String> getMatchingPortalNames(String playerName, String prefix, int limit) {
        List<String> names = new ArrayList<>();
        if (playerName == null) return names;
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase();
        for (String portalName : getPlayerPortals(playerName).keySet()) {
            if (portalName != null && portalName.toLowerCase().startsWith(normalizedPrefix)) {
                names.add(portalName);
                if (limit > 0 && names.size() >= limit) break;
            }
        }
        return names;
    }

    // ── Portal blocks (spatial index) ────────────────────────────────────────

    public Map<String, PortalDataBlocks> getPlayerPortalsBlocks() {
        return playerPortalsBlocks;
    }

    public void putPortalBlock(String key, PortalDataBlocks blockData) {
        if (key == null || blockData == null) return;
        synchronized (portalBlockLock) {
            playerPortalsBlocks.put(key, blockData);
            String locKey = portalLocationKey(blockData.getWorld(), blockData.getX(), blockData.getY(), blockData.getZ());
            portalBlockLocationIndex.put(locKey, Boolean.TRUE);
            if (blockData.getPortalName() != null) {
                portalNameToBlockKeys.computeIfAbsent(blockData.getPortalName(), k -> ConcurrentHashMap.newKeySet())
                        .add(key);
            }
        }
    }

    /** O(1) check — avoids O(n) iteration over playerPortalsBlocks.values(). */
    public boolean isPortalBlockAtLocation(String world, int x, int y, int z) {
        return portalBlockLocationIndex.containsKey(portalLocationKey(world, x, y, z));
    }

    /** Clears all block maps and both indices atomically. */
    public void clearPortalBlocks() {
        synchronized (portalBlockLock) {
            playerPortalsBlocks.clear();
            portalBlockLocationIndex.clear();
            portalNameToBlockKeys.clear();
        }
    }

    /**
     * Removes all blocks belonging to {@code portalName} from all maps.
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
                    portalBlockLocationIndex.remove(
                            portalLocationKey(bd.getWorld(), bd.getX(), bd.getY(), bd.getZ()));
                    removed.add(bd);
                }
            }
        }
        return removed;
    }

    private static String portalLocationKey(String world, int x, int y, int z) {
        return world + "," + x + "," + y + "," + z;
    }

    // ── Portal tasks ─────────────────────────────────────────────────────────

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
}
