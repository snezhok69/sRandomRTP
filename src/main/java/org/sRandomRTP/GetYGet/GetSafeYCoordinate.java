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
