package org.sRandomRTP.GetYGet;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Utils.AsyncChunkUtil;
import org.sRandomRTP.Utils.WorldHeightSupport;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class VerticalSafeYSearchSupport {

    /** Class-level logger — never null even during plugin reload/disable race. */
    private static final Logger LOG = Logger.getLogger(VerticalSafeYSearchSupport.class.getName());

    public interface PositionSafetyChecker {
        boolean isSafe(World world, int x, int y, int z, int minAllowedY);
    }

    private VerticalSafeYSearchSupport() {
    }

    public static CompletableFuture<Integer> getSafeYCoordinateAsync(Class<?> sourceClass,
                                                              String logPrefix,
                                                              World world,
                                                              int x,
                                                              int z,
                                                              TeleportRequestContext context,
                                                              int minAllowedY,
                                                              PositionSafetyChecker checker) {
        CompletableFuture<Integer> future = new CompletableFuture<>();

        if (world == null || isOutsideBorder(world, x, z)) {
            future.complete(-1);
            return future;
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            Location location = new Location(world, x, WorldHeightSupport.getMinHeight(world), z);
            FoliaSchedulerFacade.runAtLocation(location, () -> completeSearch(sourceClass, logPrefix, future, world, x, z, context, minAllowedY, checker, true));
            return future;
        }

        AsyncChunkUtil.requestChunk(world, x >> 4, z >> 4).whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                LoggerUtility.loggerUtility(sourceClass, throwable);
                future.complete(-1);
                return;
            }
            if (chunk == null || !chunk.isLoaded()) {
                future.complete(-1);
                return;
            }
            Location location = new Location(world, x, WorldHeightSupport.getMinHeight(world), z);
            FoliaSchedulerFacade.runAtLocation(location, () -> completeSearch(sourceClass, logPrefix, future, world, x, z, context, minAllowedY, checker, false));
        });

        return future;
    }

    public static int getSafeYCoordinateOnLoadedChunk(Class<?> sourceClass,
                                               String logPrefix,
                                               World world,
                                               int x,
                                               int z,
                                               TeleportRequestContext context,
                                               int minAllowedY,
                                               PositionSafetyChecker checker) {
        try {
            if (context != null && context.isInactive()) {
                return -1;
            }

            boolean loggingEnabled = Variables.isLoggingEnabled();
            long startedAt = System.nanoTime();
            int result = search(world, x, z, WorldHeightSupport.getMinHeight(world), world.getMaxHeight() - 1,
                    context, loggingEnabled, minAllowedY, logPrefix, checker);
            if (Variables.getTeleportMetrics() != null) {
                Variables.getTeleportMetrics().recordSafeYSearch(System.nanoTime() - startedAt);
            }
            return result;
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(sourceClass, e);
            return -1;
        }
    }

    private static void completeSearch(Class<?> sourceClass,
                                       String logPrefix,
                                       CompletableFuture<Integer> future,
                                       World world,
                                       int x,
                                       int z,
                                       TeleportRequestContext context,
                                       int minAllowedY,
                                       PositionSafetyChecker checker,
                                       boolean ensureChunkLoaded) {
        try {
            if (context != null && context.isInactive()) {
                future.complete(-1);
                return;
            }

            if (ensureChunkLoaded) {
                try {
                    world.getChunkAt(x >> 4, z >> 4);
                } catch (RuntimeException t) {
                    LoggerUtility.loggerUtility(sourceClass, t);
                    future.complete(-1);
                    return;
                }
            }

            boolean loggingEnabled = Variables.isLoggingEnabled();
            long startedAt = System.nanoTime();
            int result = search(world, x, z, WorldHeightSupport.getMinHeight(world), world.getMaxHeight() - 1,
                    context, loggingEnabled, minAllowedY, logPrefix, checker);
            if (Variables.getTeleportMetrics() != null) {
                Variables.getTeleportMetrics().recordSafeYSearch(System.nanoTime() - startedAt);
            }
            future.complete(result);
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(sourceClass, e);
            future.complete(-1);
        }
    }

    private static int search(World world, int x, int z, int minY, int maxY,
                              TeleportRequestContext context, boolean loggingEnabled,
                              int minAllowedY, String logPrefix, PositionSafetyChecker checker) {
        if (checker.isSafe(world, x, maxY, z, minAllowedY)) {
            if (loggingEnabled) {
                LOG.info(logPrefix + " Safe Y-coordinate found at top: " + maxY);
            }
            return maxY;
        }

        int low = minY;
        int high = maxY;

        while (low <= high) {
            if (shouldAbort(context, loggingEnabled, logPrefix)) {
                return -1;
            }

            int mid = low + (high - low) / 2;
            if (checker.isSafe(world, x, mid, z, minAllowedY)) {
                if (loggingEnabled) {
                    LOG.info(logPrefix + " Safe Y-coordinate found: " + mid);
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

        return linearSearch(world, x, z, minY, maxY, context, loggingEnabled, minAllowedY, logPrefix, checker);
    }

    private static int linearSearch(World world, int x, int z, int minY, int maxY,
                                    TeleportRequestContext context, boolean loggingEnabled,
                                    int minAllowedY, String logPrefix, PositionSafetyChecker checker) {
        for (int y = maxY; y > minY; y--) {
            if (shouldAbort(context, loggingEnabled, logPrefix)) {
                return -1;
            }

            if (checker.isSafe(world, x, y, z, minAllowedY)) {
                if (loggingEnabled) {
                    LOG.info(logPrefix + " Safe Y-coordinate found with linear search: " + y);
                }
                return y;
            }
        }

        if (loggingEnabled) {
            LOG.warning(logPrefix + " Failed to find a safe Y-coordinate at the point X:" + x + ", Z:" + z);
        }
        return -1;
    }

    private static boolean shouldAbort(TeleportRequestContext context, boolean loggingEnabled, String logPrefix) {
        if (context != null && context.isInactive()) {
            if (loggingEnabled) {
                LOG.fine(logPrefix + " Aborting height search due to request cancellation");
            }
            return true;
        }
        return false;
    }

    private static boolean isOutsideBorder(World world, int x, int z) {
        WorldBorder border = world.getWorldBorder();
        if (border == null) {
            return false;
        }
        double halfSize = border.getSize() / 2.0D;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();
        return Math.abs(x - centerX) > halfSize || Math.abs(z - centerZ) > halfSize;
    }
}
