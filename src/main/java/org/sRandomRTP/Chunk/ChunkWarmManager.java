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
import org.sRandomRTP.DifferentMethods.Variables;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class ChunkWarmManager implements Listener {
    private static ChunkWarmManager instance;

    private final JavaPlugin plugin;
    private final Set<CompletableFuture<?>> inflightLoads = ConcurrentHashMap.newKeySet();
    private final Map<UUID, long[]> lastWarmedPlayerChunk = new ConcurrentHashMap<>();

    private WrappedTask warmTask;
    private boolean listenerRegistered;

    private boolean enabled;
    private boolean warmSpawns;
    private boolean warmPlayers;
    private boolean triggerOnWorldLoad;
    private boolean triggerOnJoin;
    private boolean triggerOnMove;
    private int warmRadius;
    private int loadsPerTickBudget;
    private long warmPeriodTicks;
    private int maxInflightLoads;
    private double tpsPauseThreshold;

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

    public synchronized void reload(FileConfiguration chunkfile) {
        if (Variables.chunkfile == null) {
            return;
        }

        this.enabled = Variables.chunkfile.getBoolean("chunk-warming.enabled", false);
        this.warmRadius = Math.max(0, Variables.chunkfile.getInt("chunk-warming.warm-radius", 1));
        this.loadsPerTickBudget = Math.max(0, Variables.chunkfile.getInt("chunk-warming.loads-per-tick-budget", 24));
        this.warmPeriodTicks = Math.max(1L, Variables.chunkfile.getLong("chunk-warming.warm-period-ticks", 20L));
        this.maxInflightLoads = Math.max(1, Variables.chunkfile.getInt("chunk-warming.max-inflight-loads", 64));
        this.tpsPauseThreshold = Variables.chunkfile.getDouble("chunk-warming.tps-pause-threshold", 18.5D);
        this.warmSpawns = Variables.chunkfile.getBoolean("chunk-warming.warm-spawn-locations", true);
        this.warmPlayers = Variables.chunkfile.getBoolean("chunk-warming.warm-player-locations", true);
        this.triggerOnWorldLoad = Variables.chunkfile.getBoolean("chunk-warming.trigger-on-world-load", true);
        this.triggerOnJoin = Variables.chunkfile.getBoolean("chunk-warming.trigger-on-player-join", true);
        this.triggerOnMove = Variables.chunkfile.getBoolean("chunk-warming.trigger-on-player-move", true);

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
        warmTask = Variables.getFoliaLib().getImpl().runTimerAsync(() -> {
            try {
                runWarmCycle();
            } catch (Throwable throwable) {
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

    private void disableWarmth() {
        cancelWarmTask();
        lastWarmedPlayerChunk.clear();
        inflightLoads.clear();
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

        if (tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold)) {
            return;
        }

        if (getInflight() >= maxInflightLoads) {
            return;
        }

        AtomicInteger budget = new AtomicInteger(loadsPerTickBudget);

        if (warmSpawns) {
            for (World world : Bukkit.getWorlds()) {
                if (!enabled || budget.get() <= 0) {
                    return;
                }
                if (tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold)) {
                    return;
                }
                if (getInflight() >= maxInflightLoads) {
                    return;
                }

                Location spawn = world.getSpawnLocation();
                warmArea(world, spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4, warmRadius, budget);
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
                if (tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold)) {
                    return;
                }
                if (getInflight() >= maxInflightLoads) {
                    return;
                }

                Location location = player.getLocation();
                int chunkX = location.getBlockX() >> 4;
                int chunkZ = location.getBlockZ() >> 4;
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

        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    if (budget.get() <= 0) {
                        return;
                    }
                    if (tpsPauseThreshold > 0 && tpsBelow(tpsPauseThreshold)) {
                        return;
                    }
                    if (getInflight() >= maxInflightLoads) {
                        return;
                    }

                    int chunkX = centerChunkX + dx;
                    int chunkZ = centerChunkZ + dz;

                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        continue;
                    }

                    if (!isChunkGeneratedSafe(world, chunkX, chunkZ)) {
                        continue;
                    }

                    budget.decrementAndGet();
                    CompletableFuture<?> future = PaperLib.getChunkAtAsync(world, chunkX, chunkZ, false)
                            .exceptionally(throwable -> null);
                    inflightLoads.add(future);
                    future.whenComplete((chunk, throwable) -> inflightLoads.remove(future));
                }
            }
        }
    }

    private boolean isChunkGeneratedSafe(World world, int chunkX, int chunkZ) {
        try {
            Method method = world.getClass().getMethod("isChunkGenerated", int.class, int.class);
            Object result = method.invoke(world, chunkX, chunkZ);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return false;
        } catch (NoSuchMethodException ignored) {
            return world.isChunkLoaded(chunkX, chunkZ);
        } catch (Throwable throwable) {
            LoggerUtility.loggerUtility(ChunkWarmManager.class.getName(), throwable);
            return false;
        }
    }

    private boolean tpsBelow(double threshold) {
        try {
            Object server = Bukkit.getServer();
            Method method = server.getClass().getMethod("getTPS");
            Object value = method.invoke(server);
            if (value instanceof double[]) {
                double[] tps = (double[]) value;
                return tps.length > 0 && tps[0] < threshold;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private int getInflight() {
        inflightLoads.removeIf(CompletableFuture::isDone);
        return inflightLoads.size();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        if (!enabled || !triggerOnWorldLoad) {
            return;
        }

        Location spawn = event.getWorld().getSpawnLocation();
        warmArea(event.getWorld(), spawn.getBlockX() >> 4, spawn.getBlockZ() >> 4, warmRadius,
                new AtomicInteger(loadsPerTickBudget));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!enabled || !triggerOnJoin) {
            return;
        }

        Location location = event.getPlayer().getLocation();
        warmArea(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4, warmRadius,
                new AtomicInteger(loadsPerTickBudget));
        lastWarmedPlayerChunk.put(event.getPlayer().getUniqueId(),
                new long[]{location.getBlockX() >> 4, location.getBlockZ() >> 4});
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!enabled || !triggerOnMove) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        if ((from.getBlockX() >> 4) == (to.getBlockX() >> 4)
                && (from.getBlockZ() >> 4) == (to.getBlockZ() >> 4)) {
            return;
        }

        warmArea(to.getWorld(), to.getBlockX() >> 4, to.getBlockZ() >> 4, warmRadius,
                new AtomicInteger(loadsPerTickBudget));
        lastWarmedPlayerChunk.put(event.getPlayer().getUniqueId(),
                new long[]{to.getBlockX() >> 4, to.getBlockZ() >> 4});
    }
}