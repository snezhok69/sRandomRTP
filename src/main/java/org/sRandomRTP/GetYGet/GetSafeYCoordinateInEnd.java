package org.sRandomRTP.GetYGet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.CompletableFuture;

public class GetSafeYCoordinateInEnd {
    public static CompletableFuture<Integer> getSafeYCoordinateInEndAsync(World world, int x, int z) {
        return getSafeYCoordinateInEndAsync(world, x, z, null);
    }

    public static CompletableFuture<Integer> getSafeYCoordinateInEndAsync(World world, int x, int z,
                                                                          TeleportRequestContext context) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        if (world == null) {
            future.complete(-1);
            return future;
        }

        WorldBorder border = world.getWorldBorder();
        if (border != null) {
            double halfSize = border.getSize() / 2.0D;
            double centerX = border.getCenter().getX();
            double centerZ = border.getCenter().getZ();
            if (Math.abs(x - centerX) > halfSize || Math.abs(z - centerZ) > halfSize) {
                future.complete(-1);
                return future;
            }
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            Location location = new Location(world, x, world.getMinHeight(), z);
            RegionTaskExecutor.runAtLocation(location, () -> {
                try {
                    if (context != null && (context.isCancelled() || context.isExpired())) {
                        future.complete(-1);
                        return;
                    }

                    try {
                        world.getChunkAt(x >> 4, z >> 4);
                    } catch (Throwable t) {
                        LoggerUtility.loggerUtility(GetSafeYCoordinateInEnd.class.getName(), t);
                        future.complete(-1);
                        return;
                    }

                    boolean loggingEnabled = Variables.instance != null && Variables.instance.getConfig().getBoolean("logs", false);
                    int minY = world.getMinHeight();
                    int maxY = world.getMaxHeight() - 1;
                    int result = binarySearchSafeY(world, x, z, minY, maxY, context, loggingEnabled);
                    future.complete(result);
                } catch (Throwable e) {
                    LoggerUtility.loggerUtility(GetSafeYCoordinateInEnd.class.getName(), e);
                    future.complete(-1);
                }
            });
            return future;
        }

        AsyncChunkUtil.requestChunk(world, x >> 4, z >> 4).whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                LoggerUtility.loggerUtility(GetSafeYCoordinateInEnd.class.getName(), throwable);
                future.complete(-1);
                return;
            }

            if (chunk == null || !chunk.isLoaded()) {
                future.complete(-1);
                return;
            }

            Location location = new Location(world, x, world.getMinHeight(), z);
            RegionTaskExecutor.runAtLocation(location, () -> {
                try {
                    if (context != null && (context.isCancelled() || context.isExpired())) {
                        future.complete(-1);
                        return;
                    }

                    boolean loggingEnabled = Variables.instance != null && Variables.instance.getConfig().getBoolean("logs", false);
                    int minY = world.getMinHeight();
                    int maxY = world.getMaxHeight() - 1;
                    int result = binarySearchSafeY(world, x, z, minY, maxY, context, loggingEnabled);
                    future.complete(result);
                } catch (Throwable e) {
                    LoggerUtility.loggerUtility(GetSafeYCoordinateInEnd.class.getName(), e);
                    future.complete(-1);
                }
            });
        });

        return future;
    }

    private static int binarySearchSafeY(World world, int x, int z, int minY, int maxY, TeleportRequestContext context, boolean loggingEnabled) {
        if (isSafePosition(world, x, maxY, z)) {
            if (loggingEnabled) {
                Variables.instance.getLogger().info("[End] Safe Y-coordinate found at top: " + maxY);
            }
            return maxY;
        }

        int low = minY;
        int high = maxY;

        while (low <= high) {
            if (shouldAbort(context, loggingEnabled)) {
                return -1;
            }

            int mid = low + (high - low) / 2;

            if (isSafePosition(world, x, mid, z)) {
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("[End] Safe Y-coordinate found: " + mid);
                }
                return mid;
            }

            Block block = world.getBlockAt(x, mid, z);
            if (block.getType() == Material.AIR) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return linearSearchFromTop(world, x, z, minY, maxY, context, loggingEnabled);
    }

    private static boolean isSafePosition(World world, int x, int y, int z) {
        int configMinY = Variables.teleportfile.getInt("teleport.minY-end");
        if (y < configMinY) {
            return false;
        }

        Block block = world.getBlockAt(x, y, z);
        if (block.getType() != Material.AIR) {
            return false;
        }

        Block belowBlock = block.getRelative(BlockFace.DOWN);
        Block twoBelowBlock = belowBlock.getRelative(BlockFace.DOWN);

        return belowBlock.getType().isSolid() &&
                !belowBlock.getType().name().contains("AIR") &&
                twoBelowBlock.getType().isSolid() &&
                !twoBelowBlock.getType().name().contains("AIR");
    }

    private static int linearSearchFromTop(World world, int x, int z, int minY, int maxY, TeleportRequestContext context, boolean loggingEnabled) {
        for (int y = maxY; y > minY; y--) {
            if (shouldAbort(context, loggingEnabled)) {
                return -1;
            }

            if (isSafePosition(world, x, y, z)) {
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("[End] Safe Y-coordinate found with linear search: " + y);
                }
                return y;
            }
        }

        if (loggingEnabled) {
            Variables.instance.getLogger().warning("[End] Failed to find a safe Y-coordinate at the point X:" + x + ", Z:" + z);
        }
        return -1;
    }

    private static boolean shouldAbort(TeleportRequestContext context, boolean loggingEnabled) {
        if (context != null && (context.isCancelled() || context.isExpired())) {
            if (loggingEnabled) {
                Variables.instance.getLogger().fine("[End] Aborting height search due to request cancellation");
            }
            return true;
        }
        return false;
    }
}
