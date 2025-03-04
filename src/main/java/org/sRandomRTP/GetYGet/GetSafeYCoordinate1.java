package org.sRandomRTP.GetYGet;

import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import java.util.concurrent.CompletableFuture;

public class GetSafeYCoordinate1 {

    public static CompletableFuture<Integer> getSafeYCoordinate(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4)
                .thenApply(chunk -> {
                    try {
                        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
                            Block block = world.getBlockAt(x, y, z);
                            Block blockAbove = world.getBlockAt(x, y + 1, z);
                            Block blockTwoAbove = world.getBlockAt(x, y + 2, z);
                            if (block.getLightFromSky() > 0 && block.getType().isSolid()
                                    && blockAbove.getType() == Material.AIR
                                    && blockTwoAbove.getType() == Material.AIR) {
                                return y + 1;
                            }
                        }
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                    return -1;
                });
    }

    public static CompletableFuture<CoordinateWithBiome> getSafeYCoordinateWithAirCheck(World world, int x, int z) {
        return getSafeYCoordinate(world, x, z).thenApply(safeY -> {
            if (safeY == -1) return null;
            try {
                while (safeY < world.getMaxHeight() - 2) {
                    Block blockAbove = world.getBlockAt(x, safeY + 1, z);
                    Block blockTwoAbove = world.getBlockAt(x, safeY + 2, z);
                    if (blockAbove.getType() == Material.AIR && blockTwoAbove.getType() == Material.AIR) {
                        Biome biome = world.getBiome(x, safeY, z);
                        return new CoordinateWithBiome(safeY, biome);
                    }
                    safeY++;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static class CoordinateWithBiome {
        public final int y;
        public final Biome biome;

        public CoordinateWithBiome(int y, Biome biome) {
            this.y = y;
            this.biome = biome;
        }
        @Override
        public String toString() {
            return "CoordinateWithBiome{y=" + y + ", biome=" + biome + '}';
        }
    }
}