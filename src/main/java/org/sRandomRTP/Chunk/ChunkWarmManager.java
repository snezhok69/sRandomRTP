package org.sRandomRTP.Chunk;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Variables;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkWarmManager implements Listener {
    private static ChunkWarmManager instance;

    private final JavaPlugin plugin;
    private final AtomicInteger inflightCount = new AtomicInteger();
    private final Map<UUID, long[]> lastWarmedPlayerChunk = new ConcurrentHashMap<>();
    // Long key: ((worldIndex & 0xFFFF) << 48) | ((chunkX & 0xFFFFFF) << 24) | (chunkZ & 0xFFFFFF)
    // Avoids String allocation in the hot warm loop.
    private final Map<Long, Long> recentlyScheduledChunks = new ConcurrentHashMap<>();
    // Bounded to prevent unbounded growth when dynamic-world plugins create/delete many worlds.
    // Access is guarded by Collections.synchronizedMap so reads and writes are thread-safe.
    private final Map<String, Integer> worldIndices = java.util.Collections.synchronizedMap(
            new java.util.LinkedHashMap<String, Integer>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(java.util.Map.Entry<String, Integer> eldest) {
                    return size() > 64;
                }
            });
    private final AtomicLong worldIndexCounter = new AtomicLong();

    private WrappedTask warmTask;
    private boolean listenerRegistered;

    // volatile so the async warm task sees updates written by synchronized reload()
    private volatile boolean enabled;
    private volatile boolean warmSpawns;
    private volatile boolean warmPlayers;
    private volatile boolean triggerOnWorldLoad;
    private volatile boolean triggerOnJoin;
    private volatile boolean triggerOnMove;
    private volatile int warmRadius;
    private volatile int loadsPerTickBudget;
    private volatile long warmPeriodTicks;
    private volatile int maxInflightLoads;
    private volatile double tpsPauseThreshold;
    private volatile long cachedMaxAge = 5000L;

    private ChunkWarmManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public static synchronized ChunkWarmManager getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new ChunkWarmManager(plugin);
        }
        return instance;
    }

    public static synchronized void shutdown() {
        if (instance != null) {
            instance.disableWarmth();
        }
    }

    /**
     * Drops any per-player chunk-warm tracking for the given UUID. Called by the
     * quit listener so the lastWarmedPlayerChunk map does not retain entries for
     * disconnected players.
     */
    public static void removePlayer(UUID playerId) {
        if (playerId == null || instance == null) {
            return;
        }
        instance.lastWarmedPlayerChunk.remove(playerId);
    }

    public synchronized void reload(FileConfiguration chunkfile) {
        FileConfiguration cfg = chunkfile;
        if (Variables.getPluginContext() != null
                && Variables.getPluginContext().getConfigRegistry() != null
                && Variables.getPluginContext().getConfigRegistry().getChunkFile() != null) {
            cfg = Variables.getPluginContext().getConfigRegistry().getChunkFile();
        }
        if (cfg == null) {
            return;
        }

        this.enabled = cfg.getBoolean("chunk-warming.enabled", false);
        this.warmRadius = Math.max(0, cfg.getInt("chunk-warming.warm-radius", 1));
        this.loadsPerTickBudget = Math.max(0, cfg.getInt("chunk-warming.loads-per-tick-budget", 24));
        this.warmPeriodTicks = Math.max(1L, cfg.getLong("chunk-warming.warm-period-ticks", 20L));
        this.maxInflightLoads = Math.max(1, cfg.getInt("chunk-warming.max-inflight-loads", 64));
        this.tpsPauseThreshold = cfg.getDouble("chunk-warming.tps-pause-threshold", 18.5D);
        this.cachedMaxAge = Math.max(5000L, this.warmPeriodTicks * 100L);
        this.warmSpawns = cfg.getBoolean("chunk-warming.warm-spawn-locations", true);
        this.warmPlayers = cfg.getBoolean("chunk-warming.warm-player-locations", true);
        this.triggerOnWorldLoad = cfg.getBoolean("chunk-warming.trigger-on-world-load", true);
        this.triggerOnJoin = cfg.getBoolean("chunk-warming.trigger-on-player-join", true);
        this.triggerOnMove = cfg.getBoolean("chunk-warming.trigger-on-player-move", true);

        if (!enabled || loadsPerTickBudget <= 0) {
            disableWarmth();
            return;
        }

        registerListenerIfNeeded();
        scheduleWarmTask();
    }

    private void registerListenerIfNeeded() {
        if (!listenerRegistered) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
            listenerRegistered = true;
        }
    }

    private void scheduleWarmTask() {
        cancelWarmTask();
        warmTask = FoliaSchedulerFacade.runTimer(() -> {
            try {
                runWarmCycle();
            } catch (RuntimeException throwable) {
                LoggerUtility.loggerUtility(ChunkWarmManager.class.getName(), throwable);
            }
        }, warmPeriodTicks, warmPeriodTicks);
    }

    private void cancelWarmTask() {
        if (warmTask != null) {
            warmTask.cancel();
            warmTask = null;
        }
    }

    private synchronized void disableWarmth() {
        cancelWarmTask();
        lastWarmedPlayerChunk.clear();
        recentlyScheduledChunks.clear();
        inflightCount.set(0);
        if (listenerRegistered) {
            HandlerList.unregisterAll(this);
            listenerRegistered = false;
        }
        enabled = false;
    }

    private void runWarmCycle() {
        if (!enabled) {
            return;
        }

        // Snapshot volatile fields once to avoid races with concurrent reload() and to
        // prevent redundant TPS reads across the spawn/player loops below.
        final boolean pauseForTps = tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold);
        if (pauseForTps) {
            return;
        }

        if (getInflight() >= maxInflightLoads) {
            return;
        }

        // Proactively evict expired entries so the map does not accumulate stale chunks
        // between visits (entries are removed lazily in isRecentlyScheduled, but this
        // cleans up chunks that are never revisited).
        evictExpiredScheduledChunks();

        AtomicInteger budget = new AtomicInteger(loadsPerTickBudget);

        if (warmSpawns) {
            for (World world : Bukkit.getWorlds()) {
                if (!enabled || budget.get() <= 0) {
                    return;
                }
                if (getInflight() >= maxInflightLoads) {
                    return;
                }

                Location spawn = world.getSpawnLocation();
                warmArea(world, toChunkCoord(spawn.getBlockX()), toChunkCoord(spawn.getBlockZ()), warmRadius, budget);
            }
        }

        if (warmPlayers && budget.get() > 0) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline()) {
                    continue;
                }
                if (!enabled || budget.get() <= 0) {
                    return;
                }
                if (getInflight() >= maxInflightLoads) {
                    return;
                }

                Location location = player.getLocation();
                int chunkX = toChunkCoord(location.getBlockX());
                int chunkZ = toChunkCoord(location.getBlockZ());
                long[] last = lastWarmedPlayerChunk.get(player.getUniqueId());
                if (last != null && last[0] == chunkX && last[1] == chunkZ) {
                    continue;
                }

                warmArea(location.getWorld(), chunkX, chunkZ, warmRadius, budget);
                lastWarmedPlayerChunk.put(player.getUniqueId(), new long[]{chunkX, chunkZ});
            }
        }
    }

    private void warmArea(World world, int centerChunkX, int centerChunkZ, int radius, AtomicInteger budget) {
        if (world == null || budget.get() <= 0) {
            return;
        }
        // Check TPS once before the loop — avoids repeated TPS reads per chunk
        if (tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold)) {
            return;
        }

        outer:
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    if (budget.get() <= 0 || getInflight() >= maxInflightLoads) {
                        break outer;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;

                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }

                    if (isRecentlyScheduled(world, chunkX, chunkZ)) {
                        continue;
                    }

                    if (!isChunkGeneratedSafe(world, chunkX, chunkZ)) {
                        continue;
                    }

                    budget.decrementAndGet();
                    CompletableFuture<?> future = PaperLib.getChunkAtAsync(world, chunkX, chunkZ, false)
                            .exceptionally(throwable -> null);
                    recentlyScheduledChunks.put(chunkKeyLong(world, chunkX, chunkZ), System.currentTimeMillis());
                    inflightCount.incrementAndGet();
                    future.whenComplete((chunk, throwable) -> inflightCount.updateAndGet(c -> c <= 0 ? 0 : c - 1));
                }
            }
        }
    }

    private boolean isChunkGeneratedSafe(World world, int chunkX, int chunkZ) {
        // PaperLib.isChunkGenerated() handles reflection/fallback internally and is kept
        return PaperLib.isChunkGenerated(world, chunkX, chunkZ);
    }

    private boolean tpsBelow(double threshold) {
        double currentTps = Variables.getServerMetricsProvider().getPrimaryTps();
        return !Double.isNaN(currentTps) && currentTps < threshold;
    }

    private void evictExpiredScheduledChunks() {
        long now = System.currentTimeMillis();
        long maxAge = cachedMaxAge;
        recentlyScheduledChunks.entrySet().removeIf(e -> now - e.getValue() > maxAge);
    }

    private boolean isRecentlyScheduled(World world, int chunkX, int chunkZ) {
        long key = chunkKeyLong(world, chunkX, chunkZ);
        long now = System.currentTimeMillis();
        Long previous = recentlyScheduledChunks.get(key);
        if (previous == null) {
            return false;
        }
        if (now - previous > cachedMaxAge) {
            recentlyScheduledChunks.remove(key, previous);
            return false;
        }
        return true;
    }

    /**
     * Compact long key for a chunk — avoids String allocation in the hot warm loop.
     * Encoding: bits 63-48 = world index (16 bits), bits 47-24 = chunkX (24 bits), bits 23-0 = chunkZ (24 bits).
     * ChunkX/Z are shifted by 0x800000 to map the signed range [-8388608, 8388607] to unsigned [0, 16777215].
     */
    private long chunkKeyLong(World world, int chunkX, int chunkZ) {
        int worldIdx = worldIndices.computeIfAbsent(world.getName(),
                k -> (int) (worldIndexCounter.getAndIncrement() & 0xFFFFL));
        return ((long) (worldIdx & 0xFFFF) << 48)
                | ((long) ((chunkX + 0x800000) & 0xFFFFFF) << 24)
                | ((chunkZ + 0x800000) & 0xFFFFFF);
    }

    private static int toChunkCoord(int blockCoord) {
        return blockCoord >> 4;
    }

    private int getInflight() {
        return inflightCount.get();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (!enabled || !triggerOnWorldLoad) {
            return;
        }

        Location spawn = event.getWorld().getSpawnLocation();
        warmArea(event.getWorld(), toChunkCoord(spawn.getBlockX()), toChunkCoord(spawn.getBlockZ()), warmRadius,
                new AtomicInteger(loadsPerTickBudget));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled || !triggerOnJoin) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        int joinChunkX = toChunkCoord(location.getBlockX());
        int joinChunkZ = toChunkCoord(location.getBlockZ());
        warmArea(location.getWorld(), joinChunkX, joinChunkZ, warmRadius,
                new AtomicInteger(loadsPerTickBudget));
        lastWarmedPlayerChunk.put(event.getPlayer().getUniqueId(),
                new long[]{joinChunkX, joinChunkZ});
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled || !triggerOnMove) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        int toChunkX = toChunkCoord(to.getBlockX());
        int toChunkZ = toChunkCoord(to.getBlockZ());
        if (toChunkCoord(from.getBlockX()) == toChunkX
                && toChunkCoord(from.getBlockZ()) == toChunkZ) {
            return;
        }

        warmArea(to.getWorld(), toChunkX, toChunkZ, warmRadius,
                new AtomicInteger(loadsPerTickBudget));
        lastWarmedPlayerChunk.put(event.getPlayer().getUniqueId(),
                new long[]{toChunkX, toChunkZ});
    }
}
