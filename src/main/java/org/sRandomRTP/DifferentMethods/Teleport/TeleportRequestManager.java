package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.Locale;
import java.util.Map;
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

        long perAttemptTimeoutMillis;
        long totalTimeoutMillis;

        boolean useConfigTimeouts = Variables.teleportfile.getBoolean("teleport.teleport-timeout.enabled");

        if (useConfigTimeouts) {
            perAttemptTimeoutMillis = Variables.teleportfile.getLong("teleport.teleport-timeout.attempt-timeout-ms");
            totalTimeoutMillis = Variables.teleportfile.getLong("teleport.teleport-timeout.total-timeout-ms");
        } else {
            perAttemptTimeoutMillis = Long.MAX_VALUE;
            totalTimeoutMillis = Long.MAX_VALUE;
        }

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
        ACTIVE_REQUESTS.put(playerId, context);
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
        Variables.getRuntimeState().putTeleportTask(player.getUniqueId(), task);
        TeleportRequestContext context = ACTIVE_REQUESTS.get(player.getUniqueId());
        if (context != null) {
            context.trackTask(task);
        }
    }

    private static void cancelRegisteredTask(UUID playerId, boolean loggingEnabled) {
        RuntimeStateRegistry state = Variables.getRuntimeState();
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
}
