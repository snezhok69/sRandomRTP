package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.CompletableFuture;

public class TeleportWithChunkLoading {

    public static void teleportWithChunkLoading(Player player, Location teleportLocation,
                                                boolean loggingEnabled,
                                                int finalNewX, int finalNewZ, int newY) {
        if (player == null || teleportLocation == null || teleportLocation.getWorld() == null) {
            if (loggingEnabled) {
                Bukkit.getLogger().severe("[TeleportWithChunkLoading] Cannot teleport because target data is missing");
            }
            return;
        }
        if (!player.isOnline()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[TeleportWithChunkLoading] Player " + player.getName() + " went offline before teleportation");
            }
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            return;
        }

        // Build the final teleport runnable once — Folia wraps it in runAtLocation, Paper runs it directly.
        final Runnable doTeleport = () ->
                PerformTeleport.performTeleport(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY);

        final boolean isFolia = Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia();
        final Runnable scheduledAction;
        if (isFolia) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[TeleportWithChunkLoading] (Folia) Loading chunk async before region task");
            }
            scheduledAction = () -> {
                // On Folia we must schedule the teleport through the region owning the location.
                CompletableFuture<Void> taskFuture = Variables.getFoliaLib().getImpl()
                        .runAtLocation(teleportLocation.clone(), ignored -> {
                            FoliaSchedulerFacade.runAtEntity(player, doTeleport);
                        });
                TeleportRequestContext ctxInner = TeleportRequestManager.getContext(player.getUniqueId());
                if (ctxInner != null) {
                    ctxInner.trackFuture(taskFuture);
                }
            };
        } else {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[TeleportWithChunkLoading] (Non-Folia) Ensuring chunk is loaded before teleport");
            }
            scheduledAction = () -> FoliaSchedulerFacade.runAtEntity(player, doTeleport);
        }

        World world   = teleportLocation.getWorld();
        int chunkX    = teleportLocation.getBlockX() >> 4;
        int chunkZ    = teleportLocation.getBlockZ() >> 4;
        executeAfterChunkLoad(player, world, chunkX, chunkZ, loggingEnabled, scheduledAction);
    }

    /**
     * Loads the chunk at {@code (chunkX, chunkZ)} asynchronously, then — once the chunk is ready —
     * calls {@code onChunkReady} on the async completion thread.
     *
     * <p>All common guards (throwable, context cancelled, chunk null, player offline) are
     * handled here so the caller only provides the scheduler-specific teleport action.</p>
     */
    private static void executeAfterChunkLoad(Player player, World world, int chunkX, int chunkZ,
                                              boolean loggingEnabled, Runnable onChunkReady) {
        CompletableFuture<Chunk> chunkFuture = AsyncChunkUtil.requestChunk(world, chunkX, chunkZ);
        TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
        if (context != null) {
            context.trackFuture(chunkFuture);
        }

        chunkFuture.whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                if (loggingEnabled) {
                    Bukkit.getLogger().severe("[TeleportWithChunkLoading] Failed to load chunk async: " + throwable.getMessage());
                }
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                return;
            }

            if (context != null && context.isInactive()) {
                if (loggingEnabled) {
                    Bukkit.getLogger().info("[TeleportWithChunkLoading] Aborting: context cancelled/expired for " + player.getName());
                }
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                return;
            }

            if (chunk == null || !chunk.isLoaded()) {
                if (loggingEnabled) {
                    Bukkit.getLogger().warning("[TeleportWithChunkLoading] Chunk not loaded after async request; aborting");
                }
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                return;
            }

            if (!player.isOnline()) {
                if (loggingEnabled) {
                    Bukkit.getLogger().info("[TeleportWithChunkLoading] Player went offline before teleport after chunk load");
                }
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                return;
            }

            onChunkReady.run();
        });
    }
}
