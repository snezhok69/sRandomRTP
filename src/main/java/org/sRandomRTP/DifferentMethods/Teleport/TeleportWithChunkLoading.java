package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.CompletableFuture;

public class TeleportWithChunkLoading {
    public static void teleportWithChunkLoading(Player player, Location teleportLocation, boolean loggingEnabled, int finalNewX, int finalNewZ, int newY) {
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

        World world = teleportLocation.getWorld();
        final int chunkX = teleportLocation.getBlockX() >> 4;
        final int chunkZ = teleportLocation.getBlockZ() >> 4;

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[TeleportWithChunkLoading] (Folia) Scheduling region task to load chunk [" + chunkX + ", " + chunkZ + "]");
            }

            java.util.concurrent.CompletableFuture<Void> taskFuture = Variables.getFoliaLib().getImpl().runAtLocation(teleportLocation.clone(), ignored -> {
                try {
                    TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
                    if (context != null && (context.isCancelled() || context.isExpired())) {
                        if (loggingEnabled) {
                            Bukkit.getLogger().info("[TeleportWithChunkLoading] Aborting due to context cancelled/expired for player " + player.getName());
                        }
                        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                        return;
                    }

                    try {
                        world.getChunkAt(chunkX, chunkZ);
                    } catch (Throwable t) {
                        if (loggingEnabled) {
                            Bukkit.getLogger().severe("[TeleportWithChunkLoading] Failed to load chunk for teleport: " + t.getMessage());
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

                    RegionTaskExecutor.runAtEntity(player, () ->
                            PerformTeleport.performTeleport(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY));
                } catch (Throwable t) {
                    if (loggingEnabled) {
                        Bukkit.getLogger().severe("[TeleportWithChunkLoading] Unexpected error in region task: " + t.getMessage());
                    }
                    CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                }
            });
            TeleportRequestContext ctx = TeleportRequestManager.getContext(player.getUniqueId());
            if (ctx != null) {
                ctx.trackFuture(taskFuture);
            }
            return;
        }

        if (loggingEnabled) {
            Bukkit.getLogger().info("[TeleportWithChunkLoading] (Non-Folia) Ensuring chunk is loaded before teleport");
        }
        CompletableFuture<Chunk> future = AsyncChunkUtil.requestChunk(world, chunkX, chunkZ);
        TeleportRequestContext context = TeleportRequestManager.getContext(player.getUniqueId());
        if (context != null) {
            context.trackFuture(future);
        }

        future.whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                if (loggingEnabled) {
                    Bukkit.getLogger().severe("[TeleportWithChunkLoading] Error while loading chunk: " + throwable.getMessage());
                }
                CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                return;
            }

            TeleportRequestContext ctx = TeleportRequestManager.getContext(player.getUniqueId());
            if (ctx != null && (ctx.isCancelled() || ctx.isExpired())) {
                if (loggingEnabled) {
                    Bukkit.getLogger().info("[TeleportWithChunkLoading] Aborting after chunk load due to cancel/expired");
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

            RegionTaskExecutor.runAtEntity(player, () ->
                    PerformTeleport.performTeleport(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY));
        });
    }
}
