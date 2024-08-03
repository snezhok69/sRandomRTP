package org.sRandomRTP.GetYGet;

import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.sRandomRTP.DifferentMethods.LoggerUtility;

import java.util.concurrent.CompletableFuture;

public class GetSafeYCoordinate {

    public static CompletableFuture<Integer> getSafeYCoordinateAsync(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenApply(chunk -> getSafeYCoordinate(world, x, z));
    }

    public static int getSafeYCoordinate(World world, int x, int z) {
        try {
            for (int y = world.getMaxHeight() - 1; y > 0; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getLightFromSky() > 0 && block.getType().isSolid()) {
                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        return y + 1;
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1;
    }

    public static class CoordinateWithBiome {
        public final int y;
        public final Biome biome;

        public CoordinateWithBiome(int y, Biome biome) {
            this.y = y;
            this.biome = biome;
        }
    }

    public static CompletableFuture<CoordinateWithBiome> getSafeYCoordinateWithAirCheckAsync(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenApply(chunk -> {
                    int y = getSafeYCoordinate(world, x, z);
                    if (y == -1) return null;

                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        Biome biome = world.getBiome(x, y, z);
                        return new CoordinateWithBiome(y, biome);
                    }
                    return null;
                });
    }

    public static CompletableFuture<Integer> getSafeYCoordinateFromBottomAsync(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenApply(chunk -> getSafeYCoordinateFromBottom(world, x, z));
    }


    // REGION \\
    public static CompletableFuture<Integer> getSafeYCoordinateFromBottomAsyncRegion(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenApply(chunk -> getSafeYCoordinateFromBottomRegion(world, x, z));
    }

    public static int getSafeYCoordinateRegion(World world, int x, int z) {
        try {
            for (int y = world.getMaxHeight() - 1; y > 0; y--) {
                Block block = world.getBlockAt(x, y, z);
                // Check if the block is solid
                if (block.getType().isSolid()) {
                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

                    // Check if the two blocks above are air
                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        // Specific checks for different worlds
                        if (world.getEnvironment() == World.Environment.NETHER) {
                            // In the Nether, avoid lava and ensure there's no lava above
                            if (block.getType() != Material.LAVA && blockAbove.getType() != Material.LAVA && blockTwoAbove.getType() != Material.LAVA) {
                                return y + 1;
                            }
                        } else if (world.getEnvironment() == World.Environment.THE_END) {
                            // In the End, avoid void areas
                            if (block.getType() != Material.VOID_AIR && block.getType() != Material.CAVE_AIR) {
                                return y + 1;
                            }
                        } else {
                            // For the normal world, ensure there's light from the sky
                            if (block.getLightFromSky() > 0) {
                                return y + 1;
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1;
    }


    public static int getSafeYCoordinateFromBottomRegion(World world, int x, int z) {
        try {
            World.Environment environment = world.getEnvironment();
            int minY = (environment == World.Environment.NETHER) ? 0 : world.getMinHeight();
            int maxY = (environment == World.Environment.NETHER) ? 128 : world.getMaxHeight() - 2;

            for (int y = minY; y < maxY; y++) {
                Block block = world.getBlockAt(x, y, z);
                if ((environment == World.Environment.NORMAL && block.getLightFromSky() > 0 && block.getType().isSolid())
                        || (environment != World.Environment.NORMAL && block.getType().isSolid())) {
                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        return y + 1;
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1;
    }

    public static CompletableFuture<CoordinateWithBiome> getSafeYCoordinateWithAirCheckAsyncRegion(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenApply(chunk -> {
                    int y = getSafeYCoordinateRegion(world, x, z);
                    if (y == -1) return null;

                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);

                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        Biome biome = world.getBiome(x, y, z);
                        return new CoordinateWithBiome(y, biome);
                    }
                    return null;
                });
    }
    // REGION \\


    public static int getSafeYCoordinateFromBottom(World world, int x, int z) {
        try {
            for (int y = 0; y < world.getMaxHeight() - 2; y++) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getLightFromSky() > 0 && block.getType().isSolid()) {
                    Block blockAbove = world.getBlockAt(x, y + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        return y + 1;
                    }
                }
            }
        } catch (Throwable e) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            String callingClassName = stackTrace[2].getClassName();
            LoggerUtility.loggerUtility(callingClassName, e);
        }
        return -1;
    }
}
