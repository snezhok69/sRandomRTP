package org.sRandomRTP.GetYGet;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.Teleport.RegionTaskExecutor;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.concurrent.CompletableFuture;

public class GetSafeYCoordinate {
    public static class CoordinateWithBiome {
        public final int x;
        public final int y;
        public final int z;
        public final Biome biome;

        public CoordinateWithBiome(int y, Biome biome) {
            this(Integer.MIN_VALUE, y, Integer.MIN_VALUE, biome);
        }

        public CoordinateWithBiome(int x, int y, int z, Biome biome) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.biome = biome;
        }
    }

    private static CoordinateWithBiome failure(int x, int z) {
        return new CoordinateWithBiome(x, -1, z, null);
    }

    public static CompletableFuture<CoordinateWithBiome> getSafeYCoordinateWithAirCheckAsync(World world, int x, int z) {
        return getSafeYCoordinateWithAirCheckAsync(world, x, z, null);
    }

    public static CompletableFuture<CoordinateWithBiome> getSafeYCoordinateWithAirCheckAsync(World world, int x, int z,
                                                                                             TeleportRequestContext context) {
        CompletableFuture<CoordinateWithBiome> future = new CompletableFuture<>();

        if (world == null) {
            future.complete(failure(x, z));
            return future;
        }

        WorldBorder border = world.getWorldBorder();
        if (border != null) {
            double halfSize = border.getSize() / 2.0D;
            double centerX = border.getCenter().getX();
            double centerZ = border.getCenter().getZ();
            if (Math.abs(x - centerX) > halfSize || Math.abs(z - centerZ) > halfSize) {
                future.complete(failure(x, z));
                return future;
            }
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            Location location = new Location(world, x, world.getMinHeight(), z);
            RegionTaskExecutor.runAtLocation(location, () -> {
                try {
                    if (context != null && (context.isCancelled() || context.isExpired())) {
                        future.complete(failure(x, z));
                        return;
                    }

                    Chunk chunk;
                    try {
                        chunk = world.getChunkAt(x >> 4, z >> 4);
                    } catch (Throwable t) {
                        LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), t);
                        future.complete(failure(x, z));
                        return;
                    }

                    boolean loggingEnabled = Variables.instance != null && Variables.instance.getConfig().getBoolean("logs", false);
                    CoordinateWithBiome result = findSafeYFast(world, chunk, x, z, context, loggingEnabled);
                    future.complete(result == null ? failure(x, z) : result);
                } catch (Throwable e) {
                    LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), e);
                    future.complete(failure(x, z));
                }
            });
            return future;
        }

        AsyncChunkUtil.requestChunk(world, x >> 4, z >> 4).whenComplete((chunk, throwable) -> {
            if (throwable != null) {
                LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), throwable);
                future.complete(failure(x, z));
                return;
            }

            if (chunk == null || !chunk.isLoaded()) {
                future.complete(failure(x, z));
                return;
            }

            Location location = new Location(world, x, world.getMinHeight(), z);
            RegionTaskExecutor.runAtLocation(location, () -> {
                try {
                    if (context != null && (context.isCancelled() || context.isExpired())) {
                        future.complete(failure(x, z));
                        return;
                    }

                    boolean loggingEnabled = Variables.instance != null && Variables.instance.getConfig().getBoolean("logs", false);
                    CoordinateWithBiome result = findSafeYFast(world, chunk, x, z, context, loggingEnabled);
                    future.complete(result == null ? failure(x, z) : result);
                } catch (Throwable e) {
                    LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), e);
                    future.complete(failure(x, z));
                }
            });
        });

        return future;
    }

    private static CoordinateWithBiome findSafeYFast(World world, Chunk chunk, int x, int z,
                                                     TeleportRequestContext context, boolean loggingEnabled) {
        int[][] candidateColumns = buildCandidateColumns(x, z);
        CoordinateWithBiome fallback = failure(x, z);

        if (world.getEnvironment() == World.Environment.NORMAL && isChunkLikelyDeepWater(world, chunk)) {
            return fallback;
        }

        for (int i = 0; i < candidateColumns.length; i++) {
            int[] column = candidateColumns[i];
            if (column == null || column.length < 2) {
                continue;
            }

            boolean allowFailureLog = loggingEnabled && (i == candidateColumns.length - 1);
            CoordinateWithBiome result = findSafeYForColumn(world, chunk, column[0], column[1], context, loggingEnabled, allowFailureLog);
            if (result != null && result.y != -1) {
                return result;
            }
            if (result != null) {
                fallback = result;
            }
        }

        return fallback;
    }

    private static int[][] buildCandidateColumns(int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int relX = x & 0xF;
        int relZ = z & 0xF;

        int[][] offsets = {
                {0, 0}, {2, 0}, {0, 2}, {-2, 0}, {0, -2},
                {2, 2}, {-2, 2}, {2, -2}, {-2, -2},
                {4, 0}, {0, 4}, {-4, 0}, {0, -4},
                {4, 2}, {-4, 2}, {4, -2}, {-4, -2}
        };

        int[][] candidates = new int[offsets.length][2];
        int count = 0;
        for (int[] offset : offsets) {
            int relCandidateX = relX + offset[0];
            int relCandidateZ = relZ + offset[1];

            if (relCandidateX < 0 || relCandidateX > 15 || relCandidateZ < 0 || relCandidateZ > 15) {
                continue;
            }

            candidates[count][0] = (chunkX << 4) + relCandidateX;
            candidates[count][1] = (chunkZ << 4) + relCandidateZ;
            count++;
        }

        if (count == 0) {
            return new int[][]{{x, z}};
        }

        int[][] trimmed = new int[count][2];
        System.arraycopy(candidates, 0, trimmed, 0, count);
        return trimmed;
    }

    private static CoordinateWithBiome findSafeYForColumn(World world, Chunk chunk, int x, int z,
                                                          TeleportRequestContext context, boolean loggingEnabled, boolean allowFailureLog) {
        World.Environment environment = world.getEnvironment();

        if (environment == World.Environment.NORMAL) {
            return findSafeYInNormalWorld(world, chunk, x, z, context, loggingEnabled, allowFailureLog);
        } else if (environment == World.Environment.NETHER) {
            return findSafeYInNether(world, chunk, x, z, context, loggingEnabled, allowFailureLog);
        } else {
            return findSafeYInOtherWorlds(world, chunk, x, z, context, loggingEnabled, allowFailureLog);
        }
    }

    private static CoordinateWithBiome findSafeYInNormalWorld(World world, Chunk chunk, int x, int z,
                                                              TeleportRequestContext context, boolean loggingEnabled, boolean allowFailureLog) {
        if (shouldAbort(context)) {
            return failure(x, z);
        }

        int maxY = world.getMaxHeight() - 1;
        int minWorldY = world.getMinHeight();
        int minConfigY = Math.max(minWorldY, Variables.teleportfile.getInt("teleport.minY"));
        int minY = Math.max(minWorldY, minConfigY);
        int relativeX = x & 0xF; // x % 16
        int relativeZ = z & 0xF; // z % 16

        int surfaceY = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES).getY();
        int oceanFloorY = world.getHighestBlockAt(x, z, HeightMap.OCEAN_FLOOR).getY();

        if (surfaceY <= minY) {
            surfaceY = minY + 1;
        }

        if (isLikelyOcean(chunk, relativeX, relativeZ, surfaceY, oceanFloorY)) {
            if (allowFailureLog && loggingEnabled) {
                Variables.instance.getLogger().fine("Skipping location at X:" + x + ", Z:" + z + " due to deep water surface.");
            }
            return failure(x, z);
        }

        int searchDepth = 12;
        int startY = Math.min(surfaceY, maxY - 1);

        for (int y = startY; y >= Math.max(surfaceY - searchDepth, minY); y--) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            if (isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)
                    && hasGentleSlope(chunk, relativeX, relativeZ, y, minY, maxY)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found near surface: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        int[] keyHeights = {62, 70, 80, 54, 45, 30};

        for (int y : keyHeights) {
            if (y >= minY && y < maxY && isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)
                    && hasGentleSlope(chunk, relativeX, relativeZ, y, minY, maxY)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found at key height: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        for (int y = maxY - 20; y > minY; y -= 10) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            Block block = chunk.getBlock(relativeX, y, relativeZ);
            if (block.getType().isSolid()) {
                for (int detailY = Math.min(y + 5, maxY); detailY >= Math.max(y - 5, minY); detailY--) {
                    if (isSafePositionFast(world, chunk, x, detailY, z, relativeX, relativeZ)
                            && hasGentleSlope(chunk, relativeX, relativeZ, detailY, minY, maxY)) {
                        Biome biome = world.getBiome(x, detailY, z);
                        if (loggingEnabled) {
                            Variables.instance.getLogger().info("Safe Y found with optimized search: " + detailY);
                        }
                        return new CoordinateWithBiome(x, detailY, z, biome);
                    }
                }
            }
        }

        if (allowFailureLog && loggingEnabled) {
            Variables.instance.getLogger().warning("Failed to find safe Y at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static CoordinateWithBiome findSafeYInNether(World world, Chunk chunk, int x, int z, TeleportRequestContext context,
                                                         boolean loggingEnabled, boolean allowFailureLog) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;

        int[] netherHeights = {31, 64, 100, 120};

        for (int y : netherHeights) {
            if (y >= minY && y < maxY && isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in Nether at key height: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        int topY = maxY - 1;
        int bottomY = minY + 1;

        while (topY > bottomY) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            if (isSafePositionFast(world, chunk, x, topY, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, topY, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in Nether from top: " + topY);
                }
                return new CoordinateWithBiome(x, topY, z, biome);
            }
            topY -= 10;

            if (isSafePositionFast(world, chunk, x, bottomY, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, bottomY, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in Nether from bottom: " + bottomY);
                }
                return new CoordinateWithBiome(x, bottomY, z, biome);
            }
            bottomY += 10;
        }

        for (int y = maxY - 1; y > minY; y -= 5) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            if (isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in Nether with detailed search: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        if (allowFailureLog && loggingEnabled) {
            Variables.instance.getLogger().warning("Failed to find safe Y in Nether at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static CoordinateWithBiome findSafeYInOtherWorlds(World world, Chunk chunk, int x, int z, TeleportRequestContext context,
                                                              boolean loggingEnabled, boolean allowFailureLog) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;

        int[] endHeights = {80, 100, 60, 40};

        for (int y : endHeights) {
            if (y >= minY && y < maxY && isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in End at key height: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        for (int y = maxY - 1; y > minY; y -= 10) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            if (isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    Variables.instance.getLogger().info("Safe Y found in End with search: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        if (allowFailureLog && loggingEnabled) {
            Variables.instance.getLogger().warning("Failed to find safe Y in End at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static boolean isSafePositionFast(World world, Chunk chunk, int x, int y, int z, int relativeX, int relativeZ) {
        int configMinY = Variables.teleportfile.getInt("teleport.minY");
        if (y < configMinY) {
            return false;
        }

        if (y + 1 >= world.getMaxHeight() || y - 1 < world.getMinHeight()) {
            return false;
        }

        Block block = chunk.getBlock(relativeX, y, relativeZ);
        if (!block.getType().isAir()) {
            return false;
        }

        Block blockAbove = chunk.getBlock(relativeX, y + 1, relativeZ);
        if (!blockAbove.getType().isAir()) {
            return false;
        }

        Block blockBelow = chunk.getBlock(relativeX, y - 1, relativeZ);
        if (!blockBelow.getType().isSolid()) {
            return false;
        }

        if (world.getEnvironment() == World.Environment.NORMAL) {
            Material belowType = blockBelow.getType();
            return !isFluidMaterial(belowType);
        } else if (world.getEnvironment() == World.Environment.NETHER) {
            Material belowType = blockBelow.getType();
            return !belowType.name().contains("LAVA");
        }
        return true;
    }

    private static boolean shouldAbort(TeleportRequestContext context) {
        return context != null && (context.isCancelled() || context.isExpired());
    }

    private static boolean isLikelyOcean(Chunk chunk, int relativeX, int relativeZ, int surfaceY, int oceanFloorY) {
        if (surfaceY <= 0) {
            return false;
        }

        int worldMinY = chunk.getWorld().getMinHeight();
        int worldMaxY = chunk.getWorld().getMaxHeight() - 1;
        int surfaceGroundY = Math.min(Math.max(surfaceY - 1, worldMinY), worldMaxY);
        int surfaceBlockY = Math.min(Math.max(surfaceY, worldMinY), worldMaxY);

        Material surfaceMaterial = chunk.getBlock(relativeX, surfaceGroundY, relativeZ).getType();
        Material blockAtSurface = chunk.getBlock(relativeX, surfaceBlockY, relativeZ).getType();

        boolean surfaceFluid = isFluidMaterial(surfaceMaterial) || isFluidMaterial(blockAtSurface);
        return surfaceFluid && surfaceY - oceanFloorY > 2;
    }

    private static boolean isChunkLikelyDeepWater(World world, Chunk chunk) {
        int chunkMinX = chunk.getX() << 4;
        int chunkMinZ = chunk.getZ() << 4;
        int[][] sampleOffsets = {{8, 8}, {4, 4}, {12, 4}, {4, 12}};
        int deepSamples = 0;

        for (int[] offset : sampleOffsets) {
            int sampleX = chunkMinX + offset[0];
            int sampleZ = chunkMinZ + offset[1];

            int surfaceY = world.getHighestBlockAt(sampleX, sampleZ, HeightMap.WORLD_SURFACE).getY();
            int oceanFloorY = world.getHighestBlockAt(sampleX, sampleZ, HeightMap.OCEAN_FLOOR).getY();

            if (surfaceY <= oceanFloorY) {
                continue;
            }

            int minHeight = world.getMinHeight();
            int maxHeight = world.getMaxHeight() - 1;
            int surfaceGroundY = Math.min(Math.max(surfaceY - 1, minHeight), maxHeight);
            int surfaceBlockY = Math.min(Math.max(surfaceY, minHeight), maxHeight);

            Material surfaceMaterial = world.getBlockAt(sampleX, surfaceGroundY, sampleZ).getType();
            Material blockAtSurface = world.getBlockAt(sampleX, surfaceBlockY, sampleZ).getType();

            if (isFluidMaterial(surfaceMaterial) || isFluidMaterial(blockAtSurface)) {
                if (surfaceY - oceanFloorY > 6) {
                    deepSamples++;
                }
            }
        }

        return deepSamples >= 2;
    }

    private static boolean hasGentleSlope(Chunk chunk, int relativeX, int relativeZ, int candidateY, int minY, int maxY) {
        int[][] offsets = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            int neighbourX = relativeX + offset[0];
            int neighbourZ = relativeZ + offset[1];

            if (neighbourX < 0 || neighbourX > 15 || neighbourZ < 0 || neighbourZ > 15) {
                continue;
            }

            int neighbourSurface = findSupportingSurfaceY(chunk, neighbourX, neighbourZ, candidateY, minY, maxY);
            if (neighbourSurface == Integer.MIN_VALUE) {
                continue;
            }

            if (Math.abs(neighbourSurface - candidateY) > 3) {
                return false;
            }
        }
        return true;
    }

    private static int findSupportingSurfaceY(Chunk chunk, int relativeX, int relativeZ, int referenceY, int minY, int maxY) {
        int highestCheck = Math.min(referenceY + 2, maxY - 1);
        int lowestCheck = Math.max(minY, referenceY - 6);

        for (int y = highestCheck; y >= lowestCheck; y--) {
            Block block = chunk.getBlock(relativeX, y, relativeZ);
            if (!block.getType().isSolid()) {
                continue;
            }

            Block above = block.getRelative(BlockFace.UP);
            Block twoAbove = above.getRelative(BlockFace.UP);

            if (above.getType().isAir() && twoAbove.getType().isAir()) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isFluidMaterial(Material type) {
        if (type == null) {
            return false;
        }

        if (type.isAir()) {
            return false;
        }

        if (type == Material.WATER || type == Material.LAVA) {
            return true;
        }

        switch (type) {
            case KELP:
            case KELP_PLANT:
            case SEAGRASS:
            case TALL_SEAGRASS:
            case BUBBLE_COLUMN:
            case VINE:
                return true;
            default:
                return false;
        }
    }
}
