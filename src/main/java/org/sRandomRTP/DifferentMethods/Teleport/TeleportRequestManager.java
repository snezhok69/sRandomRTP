package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeleportRequestManager {
    private static final Map<UUID, TeleportRequestContext> ACTIVE_REQUESTS = new ConcurrentHashMap<>();
    private static final RecentTeleportCache RECENT_CACHE = new RecentTeleportCache(20);

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
            totalTimeoutMillis = Variables.teleportfile.getLong("teleport.teleport-timeout.total-timeout-ms");  // ИСПРАВЛЕНО: было attempt-timeout-ms
        } else {
            // При false - полное отключение таймаутов
            perAttemptTimeoutMillis = Long.MAX_VALUE;
            totalTimeoutMillis = Long.MAX_VALUE;
        }

        boolean foliaEnvironment = isFoliaServer();
        if (foliaEnvironment && useConfigTimeouts) {  // ДОБАВЛЕНО: применяем минимумы только при включенных таймаутах
            perAttemptTimeoutMillis = Math.max(perAttemptTimeoutMillis, 15000L);
            totalTimeoutMillis = Math.max(totalTimeoutMillis, 60000L);
        }
        if (useConfigTimeouts && totalTimeoutMillis < perAttemptTimeoutMillis * 3) {  // ДОБАВЛЕНО: проверяем только при включенных таймаутах
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
            context.cancel(reason);
            cancelRegisteredTask(playerId, loggingEnabled);
            EconomyPaymentManager.refund(playerId);
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
        Variables.teleportTasks.put(player.getUniqueId(), task);
        TeleportRequestContext context = ACTIVE_REQUESTS.get(player.getUniqueId());
        if (context != null) {
            context.trackTask(task);
        }
    }

    private static void cancelRegisteredTask(UUID playerId, boolean loggingEnabled) {
        WrappedTask task = Variables.teleportTasks.remove(playerId);
        if (task != null && !task.isCancelled()) {
            task.cancel();
            if (loggingEnabled) {
                Bukkit.getLogger().info("Cancelled active Folia task for player " + playerId);
            }
        }
    }

    private static boolean isFoliaServer() {
        String serverName = Bukkit.getServer().getName();
        if (serverName != null && serverName.equalsIgnoreCase("Folia")) {
            return true;
        }

        String version = Bukkit.getServer().getVersion();
        return version != null && version.toLowerCase(Locale.ROOT).contains("folia");
    }
}
