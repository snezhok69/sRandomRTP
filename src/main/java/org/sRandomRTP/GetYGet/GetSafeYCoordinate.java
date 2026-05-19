package org.sRandomRTP.GetYGet;

import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.sRandomRTP.BlockBiomes.BiomeBlockValidator;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.SearchPhasePolicy;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.Utils.AsyncChunkUtil;
import org.sRandomRTP.Utils.WorldHeightSupport;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class GetSafeYCoordinate {

    /** Class-level logger — never null even during plugin reload/disable race. */
    private static final Logger LOG = Logger.getLogger(GetSafeYCoordinate.class.getName());

    // Static EnumSet caches for O(1) material checks — avoids repeated switch evaluation
    private static final Set<Material> FLUID_MATERIALS = EnumSet.of(
            Material.WATER, Material.LAVA,
            Material.KELP, Material.KELP_PLANT,
            Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.BUBBLE_COLUMN, Material.VINE
    );

    private static final Set<Material> UNSAFE_OCCUPANT_MATERIALS = materialSet(
            "COBWEB",
            "POWDER_SNOW",
            "FIRE",
            "SOUL_FIRE",
            "CACTUS",
            "CAMPFIRE",
            "SOUL_CAMPFIRE",
            "MAGMA_BLOCK"
    );

    private static final Set<Material> UNSAFE_SUPPORT_MATERIALS = EnumSet.of(
            Material.CACTUS,
            Material.CAMPFIRE,
            Material.SOUL_CAMPFIRE,
            Material.MAGMA_BLOCK,
            Material.FIRE,
            Material.SOUL_FIRE
    );

    /** Bedrock variants — used by Nether safe-Y check to reject ceiling/floor positions. */
    static final Set<Material> BEDROCK_MATERIALS = EnumSet.of(Material.BEDROCK);

    /**
     * All leaf-block variants — populated once at class-load by scanning Material names.
     * Replaces the per-call {@code type.name().contains("LEAVES")} string allocation.
     */
    private static final Set<Material> LEAF_MATERIALS;
    static {
        Set<Material> leaves = EnumSet.noneOf(Material.class);
        for (Material m : Material.values()) {
            if (m.name().contains("LEAVES")) {
                leaves.add(m);
            }
        }
        LEAF_MATERIALS = leaves.isEmpty() ? Collections.emptySet() : leaves;
    }

    /** Priority Y-levels probed first in Nether to avoid the solid ceiling/floor zones. */
    private static final int[] NETHER_PRIORITY_HEIGHTS = {31, 64, 100, 120};
    /** Priority Y-levels probed first in End and other non-overworld/nether environments. */
    private static final int[] END_PRIORITY_HEIGHTS    = {80, 100, 60, 40};

    private static final int[][] PRIMARY_COLUMN_OFFSETS = {
            {0, 0}, {2, 0}, {0, 2}, {-2, 0}, {0, -2},
            {2, 2}, {-2, 2}, {2, -2}, {-2, -2}
    };
    private static final int[][] EXTENDED_COLUMN_OFFSETS = {
            {4, 0}, {0, 4}, {-4, 0}, {0, -4},
            {4, 2}, {-4, 2}, {4, -2}, {-4, -2}
    };

    private static Set<Material> materialSet(String... names) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        if (names == null) {
            return Collections.emptySet();
        }
        for (String name : names) {
            if (name == null) {
                continue;
            }
            try {
                materials.add(Material.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // Newer materials, such as POWDER_SNOW, do not exist on 1.16.
            }
        }
        return materials.isEmpty() ? Collections.emptySet() : materials;
    }

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
            Location location = new Location(world, x, WorldHeightSupport.getMinHeight(world), z);
            FoliaSchedulerFacade.runAtLocation(location, () -> {
                try {
                    if (context != null && context.isInactive()) {
                        future.complete(failure(x, z));
                        return;
                    }

                    Chunk chunk;
                    try {
                        chunk = world.getChunkAt(x >> 4, z >> 4);
                    } catch (RuntimeException t) {
                        LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), t);
                        future.complete(failure(x, z));
                        return;
                    }

                    CoordinateWithBiome result = findSafeYOnLoadedChunk(world, chunk, x, z, context);
                    future.complete(result == null ? failure(x, z) : result);
                } catch (RuntimeException e) {
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

            Location location = new Location(world, x, WorldHeightSupport.getMinHeight(world), z);
            FoliaSchedulerFacade.runAtLocation(location, () -> {
                try {
                    if (context != null && context.isInactive()) {
                        future.complete(failure(x, z));
                        return;
                    }

                    CoordinateWithBiome result = findSafeYOnLoadedChunk(world, chunk, x, z, context);
                    future.complete(result == null ? failure(x, z) : result);
                } catch (RuntimeException e) {
                    LoggerUtility.loggerUtility(GetSafeYCoordinate.class.getName(), e);
                    future.complete(failure(x, z));
                }
            });
        });

        return future;
    }

    public static CoordinateWithBiome findSafeYOnLoadedChunk(World world, Chunk chunk, int x, int z,
                                                             TeleportRequestContext context) {
        long startedAt = System.nanoTime();
        try {
            boolean loggingEnabled = Variables.isLoggingEnabled();
            CoordinateWithBiome result = findSafeYFast(world, chunk, x, z, context, loggingEnabled);
            return result == null ? failure(x, z) : result;
        } finally {
            if (Variables.getTeleportMetrics() != null) {
                Variables.getTeleportMetrics().recordSafeYSearch(System.nanoTime() - startedAt);
            }
        }
    }

    private static CoordinateWithBiome findSafeYFast(World world, Chunk chunk, int x, int z,
                                                     TeleportRequestContext context, boolean loggingEnabled) {
        if (world.getEnvironment() == World.Environment.NORMAL && isChunkLikelyDeepWater(world, chunk)) {
            return failure(x, z);
        }

        CoordinateWithBiome primaryPassResult = searchCandidateColumns(world, chunk, x, z, context,
                loggingEnabled, buildCandidateColumns(x, z, false), false);
        if (primaryPassResult != null && primaryPassResult.y != -1) {
            return primaryPassResult;
        }

        if (!shouldUseExpandedColumnPass(context)) {
            return primaryPassResult == null ? failure(x, z) : primaryPassResult;
        }

        if (context != null && context.isInactive()) {
            return primaryPassResult != null ? primaryPassResult : failure(x, z);
        }

        CoordinateWithBiome extendedPassResult = searchCandidateColumns(world, chunk, x, z, context,
                loggingEnabled, buildCandidateColumns(x, z, true), true);
        if (extendedPassResult != null && extendedPassResult.y != -1) {
            return extendedPassResult;
        }

        return extendedPassResult != null ? extendedPassResult
                : (primaryPassResult != null ? primaryPassResult : failure(x, z));
    }

    private static CoordinateWithBiome searchCandidateColumns(World world, Chunk chunk, int x, int z,
                                                              TeleportRequestContext context, boolean loggingEnabled,
                                                              int[][] candidateColumns, boolean allowFailureLogOnLastOnly) {
        CoordinateWithBiome fallback = failure(x, z);
        for (int i = 0; i < candidateColumns.length; i++) {
            int[] column = candidateColumns[i];
            if (column == null || column.length < 2) {
                continue;
            }

            boolean allowFailureLog = allowFailureLogOnLastOnly && loggingEnabled && (i == candidateColumns.length - 1);
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

    static boolean shouldUseExpandedColumnPass(TeleportRequestContext context) {
        if (context == null) {
            return true;
        }
        if (SearchPhasePolicy.shouldReduceChunkPressure()) {
            return false;
        }
        return context.getElapsedMillis() < 2500L && context.getAttemptCount() <= 4;
    }

    static int[][] buildCandidateColumns(int x, int z, boolean includeExtendedOffsets) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        int relX = x & 0xF;
        int relZ = z & 0xF;

        // Pre-allocate the maximum possible size to avoid ArrayList + toArray overhead.
        // Each iteration writes directly into the array — no intermediate List, no per-element allocation.
        int maxSize = PRIMARY_COLUMN_OFFSETS.length
                + (includeExtendedOffsets ? EXTENDED_COLUMN_OFFSETS.length : 0);
        int[][] result = new int[maxSize][2];
        int count = 0;

        for (int[] offset : PRIMARY_COLUMN_OFFSETS) {
            int relCandidateX = relX + offset[0];
            int relCandidateZ = relZ + offset[1];
            if (relCandidateX >= 0 && relCandidateX <= 15 && relCandidateZ >= 0 && relCandidateZ <= 15) {
                result[count][0] = (chunkX << 4) + relCandidateX;
                result[count][1] = (chunkZ << 4) + relCandidateZ;
                count++;
            }
        }

        if (includeExtendedOffsets) {
            for (int[] offset : EXTENDED_COLUMN_OFFSETS) {
                int relCandidateX = relX + offset[0];
                int relCandidateZ = relZ + offset[1];
                if (relCandidateX >= 0 && relCandidateX <= 15 && relCandidateZ >= 0 && relCandidateZ <= 15) {
                    result[count][0] = (chunkX << 4) + relCandidateX;
                    result[count][1] = (chunkZ << 4) + relCandidateZ;
                    count++;
                }
            }
        }

        if (count == 0) {
            return new int[][]{{x, z}};
        }
        // Only copy if some slots were filtered out; otherwise return the full array as-is.
        return count == maxSize ? result : Arrays.copyOf(result, count);
    }

    private static int[][] buildCandidateColumns(int x, int z) {
        return buildCandidateColumns(x, z, true);
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
        int minWorldY = WorldHeightSupport.getMinHeight(world);
        // Capture once — avoids a volatile memory-barrier on every iteration of the Y scan loop
        int minY = Math.max(minWorldY, Variables.configCache.minY);
        int relativeX = x & 0xF; // x % 16
        int relativeZ = z & 0xF; // z % 16

        int surfaceY = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES).getY();
        int oceanFloorY = world.getHighestBlockAt(x, z, HeightMap.OCEAN_FLOOR).getY();

        if (surfaceY <= minY) {
            surfaceY = minY + 1;
        }

        Material topSurfaceMaterial = chunk.getBlock(relativeX, clampY(surfaceY, minWorldY, maxY), relativeZ).getType();
        if (isFluidMaterial(topSurfaceMaterial) || BiomeBlockValidator.isBlockBanned(topSurfaceMaterial)) {
            if (allowFailureLog && loggingEnabled) {
                LOG.fine("Skipping location at X:" + x + ", Z:" + z
                        + " because the top surface block is unsafe: " + topSurfaceMaterial);
            }
            return failure(x, z);
        }

        if (isLikelyOcean(chunk, relativeX, relativeZ, surfaceY, oceanFloorY)) {
            if (allowFailureLog && loggingEnabled) {
                LOG.fine("Skipping location at X:" + x + ", Z:" + z + " due to deep water surface.");
            }
            return failure(x, z);
        }

        int startY = Math.min(surfaceY + 1, maxY - 1);
        int endY = Math.max(minY, Math.max(surfaceY - 2, oceanFloorY));

        for (int y = startY; y >= endY; y--) {
            if (shouldAbort(context)) {
                return failure(x, z);
            }

            if (isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)
                    && hasGentleSlope(chunk, relativeX, relativeZ, y, minY, maxY)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    LOG.info("Safe Y found near surface: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        if (allowFailureLog && loggingEnabled) {
            LOG.warning("Failed to find safe Y at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static CoordinateWithBiome findSafeYInNether(World world, Chunk chunk, int x, int z, TeleportRequestContext context,
                                                         boolean loggingEnabled, boolean allowFailureLog) {
        int minY = WorldHeightSupport.getMinHeight(world);
        int maxY = world.getMaxHeight() - 1;
        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;

        for (int y : NETHER_PRIORITY_HEIGHTS) {
            if (y >= minY && y < maxY && isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    LOG.info("Safe Y found in Nether at key height: " + y);
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
                    LOG.info("Safe Y found in Nether from top: " + topY);
                }
                return new CoordinateWithBiome(x, topY, z, biome);
            }
            topY -= 10;

            if (isSafePositionFast(world, chunk, x, bottomY, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, bottomY, z);
                if (loggingEnabled) {
                    LOG.info("Safe Y found in Nether from bottom: " + bottomY);
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
                    LOG.info("Safe Y found in Nether with detailed search: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        if (allowFailureLog && loggingEnabled) {
            LOG.warning("Failed to find safe Y in Nether at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static CoordinateWithBiome findSafeYInOtherWorlds(World world, Chunk chunk, int x, int z, TeleportRequestContext context,
                                                              boolean loggingEnabled, boolean allowFailureLog) {
        int minY = WorldHeightSupport.getMinHeight(world);
        int maxY = world.getMaxHeight() - 1;
        int relativeX = x & 0xF;
        int relativeZ = z & 0xF;

        for (int y : END_PRIORITY_HEIGHTS) {
            if (y >= minY && y < maxY && isSafePositionFast(world, chunk, x, y, z, relativeX, relativeZ)) {
                Biome biome = world.getBiome(x, y, z);
                if (loggingEnabled) {
                    LOG.info("Safe Y found in End at key height: " + y);
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
                    LOG.info("Safe Y found in End with search: " + y);
                }
                return new CoordinateWithBiome(x, y, z, biome);
            }
        }

        if (allowFailureLog && loggingEnabled) {
            LOG.warning("Failed to find safe Y in End at X:" + x + ", Z:" + z);
        }
        return failure(x, z);
    }

    private static boolean isSafePositionFast(World world, Chunk chunk, int x, int y, int z, int relativeX, int relativeZ) {
        // Use the locally-captured minY from the calling method via parameter passing;
        // the configCache.minY volatile is read once per search and propagated down.
        if (y < Variables.configCache.minY) {
            return false;
        }

        if (y + 1 >= world.getMaxHeight() || y - 1 < WorldHeightSupport.getMinHeight(world)) {
            return false;
        }

        Block block = chunk.getBlock(relativeX, y, relativeZ);
        if (!isSafeTeleportOccupantMaterial(block.getType())) {
            return false;
        }

        Block blockAbove = chunk.getBlock(relativeX, y + 1, relativeZ);
        if (!isSafeTeleportOccupantMaterial(blockAbove.getType())) {
            return false;
        }

        Block blockBelow = chunk.getBlock(relativeX, y - 1, relativeZ);
        if (!isSafeTeleportSupportMaterial(blockBelow.getType())) {
            return false;
        }

        if (world.getEnvironment() == World.Environment.NETHER
                && BEDROCK_MATERIALS.contains(blockBelow.getType())) {
            return false;
        }
        return true;
    }

    public static boolean isSafeTeleportOccupantMaterial(Material type) {
        if (type == null) {
            return false;
        }
        if (type.isAir()) {
            return true;
        }
        if (isFluidMaterial(type) || BiomeBlockValidator.isBlockBanned(type) || isUnsafeOccupantMaterial(type)) {
            return false;
        }
        return !type.isSolid();
    }

    public static boolean isSafeTeleportSupportMaterial(Material type) {
        if (type == null || type.isAir() || !type.isSolid()) {
            return false;
        }
        return !isFluidMaterial(type) && !BiomeBlockValidator.isBlockBanned(type) && !isUnsafeSupportMaterial(type);
    }

    private static boolean shouldAbort(TeleportRequestContext context) {
        return context != null && context.isInactive();
    }

    private static boolean isLikelyOcean(Chunk chunk, int relativeX, int relativeZ, int surfaceY, int oceanFloorY) {
        if (surfaceY <= 0) {
            return false;
        }

        int worldMinY = WorldHeightSupport.getMinHeight(chunk.getWorld());
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

            int minHeight = WorldHeightSupport.getMinHeight(world);
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
        return checkNeighbour(chunk, relativeX + 1, relativeZ,     candidateY, minY, maxY)
            && checkNeighbour(chunk, relativeX - 1, relativeZ,     candidateY, minY, maxY)
            && checkNeighbour(chunk, relativeX,     relativeZ + 1, candidateY, minY, maxY)
            && checkNeighbour(chunk, relativeX,     relativeZ - 1, candidateY, minY, maxY);
    }

    private static boolean checkNeighbour(Chunk chunk, int nx, int nz, int candidateY, int minY, int maxY) {
        if (nx < 0 || nx > 15 || nz < 0 || nz > 15) return true;
        int surface = findSupportingSurfaceY(chunk, nx, nz, candidateY, minY, maxY);
        return surface == Integer.MIN_VALUE || Math.abs(surface - candidateY) <= 3;
    }

    private static int findSupportingSurfaceY(Chunk chunk, int relativeX, int relativeZ, int referenceY, int minY, int maxY) {
        int highestCheck = Math.min(referenceY + 2, maxY - 1);
        int lowestCheck = Math.max(minY, referenceY - 6);

        for (int y = highestCheck; y >= lowestCheck; y--) {
            if (!chunk.getBlock(relativeX, y, relativeZ).getType().isSolid()) {
                continue;
            }
            // Use direct Y arithmetic instead of getRelative() API calls — avoids 2 extra object lookups per iteration
            int y1 = y + 1;
            int y2 = y + 2;
            if (y1 <= maxY && y2 <= maxY
                    && chunk.getBlock(relativeX, y1, relativeZ).getType().isAir()
                    && chunk.getBlock(relativeX, y2, relativeZ).getType().isAir()) {
                return y1;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static int clampY(int y, int minY, int maxY) {
        return Math.min(Math.max(y, minY), maxY);
    }

    private static boolean isUnsafeOccupantMaterial(Material type) {
        if (type == null) return false;
        return UNSAFE_OCCUPANT_MATERIALS.contains(type) || LEAF_MATERIALS.contains(type);
    }

    private static boolean isUnsafeSupportMaterial(Material type) {
        if (type == null) return false;
        return UNSAFE_SUPPORT_MATERIALS.contains(type);
    }

    private static boolean isFluidMaterial(Material type) {
        if (type == null || type.isAir()) return false;
        return FLUID_MATERIALS.contains(type);
    }
}
