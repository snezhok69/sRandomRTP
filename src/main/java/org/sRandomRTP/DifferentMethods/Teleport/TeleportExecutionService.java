package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TeleportExecutionService {

    private TeleportExecutionService() {
    }

    public static void execute(Player player,
                               RtpCandidateResolution resolution,
                               boolean loggingEnabled,
                               TeleportRequestContext context) {
        if (player == null || resolution == null || resolution.getWorld() == null) {
            if (loggingEnabled) {
                Bukkit.getLogger().warning("[TeleportExecutionService] Missing player or target resolution");
            }
            if (player != null) {
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            }
            return;
        }

        if (!player.isOnline()) {
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            return;
        }

        final UUID playerId = player.getUniqueId();
        final Location targetLocation = resolution.toLocation();
        final List<CompletableFuture<Chunk>> preloadFutures = GetChunksToLoad.getChunksToLoad(resolution);

        if (preloadFutures.isEmpty()) {
            dispatchFinalTeleport(player, resolution, loggingEnabled, context);
            return;
        }

        CompletableFuture<Boolean> chunkLoadFuture = GetChunksToLoad.waitForChunkLoads(preloadFutures, targetLocation, loggingEnabled);
        if (context != null) {
            context.trackFuture(chunkLoadFuture);
        }

        chunkLoadFuture.whenComplete((timedOut, throwable) -> {
            if (context != null && context.isInactive()) {
                return;
            }

            if (throwable != null && loggingEnabled) {
                Bukkit.getLogger().warning("[TeleportExecutionService] Neighbor chunk preloading failed for player "
                        + player.getName() + ": " + throwable.getMessage());
            }

            if (!player.isOnline()) {
                TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "player offline");
                RuntimeStateRegistry state = Variables.getRuntimeState();
                state.setPlayerSearching(player, false);
                return;
            }

            dispatchFinalTeleport(player, resolution, loggingEnabled, context);
        });
    }

    private static void dispatchFinalTeleport(Player player,
                                              RtpCandidateResolution resolution,
                                              boolean loggingEnabled,
                                              TeleportRequestContext context) {
        if (player == null || resolution == null) {
            return;
        }

        FoliaSchedulerFacade.runAtEntity(player, new Runnable() {
            @Override
            public void run() {
                if (context != null && context.isInactive()) {
                    CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                    return;
                }
                PerformTeleport.performTeleport(player, resolution, loggingEnabled);
            }
        });
    }
}
