package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestManager {
    private static final Map<UUID, TeleportRequestContext> ACTIVE_REQUESTS = new ConcurrentHashMap<>();
    private static final RecentTeleportCache RECENT_CACHE = new RecentTeleportCache(org.sRandomRTP.Utils.PluginConstants.RECENT_TELEPORT_CACHE_CAPACITY);
    /** Cached once at class load — server type never changes at runtime. */
    private static final boolean IS_FOLIA = detectFoliaOnce();

    private TeleportRequestManager() {
    }

    public static TeleportRequestContext beginRequest(Player player, boolean loggingEnabled) {
        UUID playerId = player.getUniqueId();
        cancelRequest(playerId, loggingEnabled, "starting new request");

        // Read from the atomic ConfigCache snapshot — no live YAML lookups on every RTP
        org.sRandomRTP.Services.ConfigCache cfg = Variables.configCache;
        boolean useConfigTimeouts = cfg.teleportTimeoutEnabled;
        long perAttemptTimeoutMillis = useConfigTimeouts ? cfg.perAttemptTimeoutMs : Long.MAX_VALUE;
        long totalTimeoutMillis      = useConfigTimeouts ? cfg.totalTimeoutMs      : Long.MAX_VALUE;

        boolean foliaEnvironment = IS_FOLIA;
        if (foliaEnvironment && useConfigTimeouts) {
            // Folia's async scheduler can have higher latency — enforce a minimum floor
            perAttemptTimeoutMillis = Math.max(perAttemptTimeoutMillis,
                    org.sRandomRTP.Utils.PluginConstants.FOLIA_MIN_PER_ATTEMPT_TIMEOUT_MS);
            totalTimeoutMillis = Math.max(totalTimeoutMillis,
                    org.sRandomRTP.Utils.PluginConstants.FOLIA_MIN_TOTAL_TIMEOUT_MS);
        }
        if (useConfigTimeouts && totalTimeoutMillis < perAttemptTimeoutMillis * 3) {
            totalTimeoutMillis = perAttemptTimeoutMillis * 3;
        }

        TeleportRequestContext context = new TeleportRequestContext(playerId, perAttemptTimeoutMillis, totalTimeoutMillis, !foliaEnvironment);
        TeleportRequestContext displaced = ACTIVE_REQUESTS.put(playerId, context);
        if (displaced != null && displaced != context) {
            // A concurrent beginRequest snuck in between our cancelRequest and our put;
            // cancel the displaced context so it doesn't leak resources.
            displaced.cancel("displaced by new request");
        }
        return context;
    }

    public static TeleportRequestContext getContext(UUID playerId) {
        return ACTIVE_REQUESTS.get(playerId);
    }

    public static void cancelRequest(UUID playerId, boolean loggingEnabled, String reason) {
        TeleportRequestContext context = ACTIVE_REQUESTS.remove(playerId);
        if (context != null) {
            try {
                context.cancel(reason);
                cancelRegisteredTask(playerId, loggingEnabled);
            } finally {
                // Refund runs even if cancel() or cancelRegisteredTask() throw
                EconomyPaymentManager.refund(playerId);
            }
            if (Variables.getTeleportMetrics() != null) {
                Variables.getTeleportMetrics().recordCancellation();
                Variables.getTeleportMetrics().logSlowRequestIfNeeded(resolvePlayerName(playerId), context, "cancelled:" + reason);
            }
            if (loggingEnabled) {
                Bukkit.getLogger().info("Cancelled teleport request for player " + playerId + " due to " + reason);
            }
        }
    }

    public static void completeRequest(TeleportRequestContext context, boolean loggingEnabled) {
        if (context == null) {
            return;
        }
        ACTIVE_REQUESTS.remove(context.getPlayerId(), context);
        context.markCompleted();
        cancelRegisteredTask(context.getPlayerId(), loggingEnabled);
        if (Variables.getTeleportMetrics() != null) {
            Variables.getTeleportMetrics().recordCompletedRequest();
            Variables.getTeleportMetrics().logSlowRequestIfNeeded(resolvePlayerName(context.getPlayerId()), context, "completed");
        }
    }

    public static boolean isLocationRecentlyUsed(UUID playerId, int chunkX, int chunkZ) {
        return RECENT_CACHE.isRecentlyUsed(playerId, chunkX, chunkZ);
    }

    public static void rememberLocation(UUID playerId, int chunkX, int chunkZ) {
        RECENT_CACHE.remember(playerId, chunkX, chunkZ);
    }

    public static void registerTask(Player player, WrappedTask task) {
        if (player == null || task == null) {
            return;
        }
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state != null) state.putTeleportTask(player.getUniqueId(), task);
        TeleportRequestContext context = ACTIVE_REQUESTS.get(player.getUniqueId());
        if (context != null) {
            context.trackTask(task);
        }
    }

    private static void cancelRegisteredTask(UUID playerId, boolean loggingEnabled) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state == null) return;
        WrappedTask task = state.removeTeleportTask(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            if (loggingEnabled) {
                Bukkit.getLogger().info("Cancelled active Folia task for player " + playerId);
            }
        }
    }

    private static boolean detectFoliaOnce() {
        String serverName = Bukkit.getServer().getName();
        if (serverName != null && serverName.equalsIgnoreCase("Folia")) {
            return true;
        }
        String version = Bukkit.getServer().getVersion();
        return version != null && version.toLowerCase(Locale.ROOT).contains("folia");
    }

    private static String resolvePlayerName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        return player != null ? player.getName() : String.valueOf(playerId);
    }

    private static final class RecentTeleportCache {
        /**
         * Entry is mutated by {@code remember()} inside a ConcurrentHashMap.compute() block
         * and read by {@code isRecentlyUsed()} outside it. Synchronizing on {@code entry}
         * in both call sites eliminates the data race on {@code lookup} and {@code order}.
         */
        private static final class Entry {
            final Deque<Long> order  = new ArrayDeque<>();
            final Set<Long>   lookup = new HashSet<>();
        }

        private final Map<UUID, Entry> recentLocations = new ConcurrentHashMap<>();
        private final int maxEntries;

        RecentTeleportCache(int maxEntries) {
            this.maxEntries = Math.max(1, maxEntries);
        }

        boolean isRecentlyUsed(UUID playerId, int chunkX, int chunkZ) {
            Entry entry = recentLocations.get(playerId);
            if (entry == null) return false;
            synchronized (entry) {
                return entry.lookup.contains(toKey(chunkX, chunkZ));
            }
        }

        void remember(UUID playerId, int chunkX, int chunkZ) {
            recentLocations.compute(playerId, (uuid, entry) -> {
                if (entry == null) entry = new Entry();
                long key = toKey(chunkX, chunkZ);
                synchronized (entry) {
                    if (entry.lookup.contains(key)) return entry;
                    if (entry.order.size() >= maxEntries) {
                        Long evicted = entry.order.removeFirst();
                        entry.lookup.remove(evicted);
                    }
                    entry.order.addLast(key);
                    entry.lookup.add(key);
                }
                return entry;
            });
        }

        void clear(UUID playerId) {
            recentLocations.remove(playerId);
        }

        private long toKey(int chunkX, int chunkZ) {
            return (((long) chunkX) << 32) ^ (chunkZ & 0xffffffffL);
        }
    }
}
