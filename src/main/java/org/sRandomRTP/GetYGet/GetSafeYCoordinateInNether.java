package org.sRandomRTP.GetYGet;

import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.Location;
import org.sRandomRTP.DifferentMethods.Variables;
import java.util.concurrent.CompletableFuture;

public class GetSafeYCoordinateInNether {

    public static CompletableFuture<Integer> getSafeYCoordinateInNetherAsync(World world, int x, int z) {
        return PaperLib.getChunkAtAsync(world, x >> 4, z >> 4, true)
                .thenCompose(chunk -> {
                    CompletableFuture<Integer> future = new CompletableFuture<>();
                    Variables.getFoliaLib().getImpl().runAtLocation(new Location(world, x, 0, z), location -> {
                        int safeY = getSafeYCoordinateInNether(world, x, z);
                        future.complete(safeY);
                    });

                    return future;
                });
    }

    public static int getSafeYCoordinateInNether(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block belowBlock = block.getRelative(BlockFace.DOWN);
            Block twoBelowBlock = belowBlock.getRelative(BlockFace.DOWN);
            if (block.getType() == Material.AIR && belowBlock.getType().isSolid() && !belowBlock.getType().name().contains("BEDROCK")
                    && twoBelowBlock.getType().isSolid() && !twoBelowBlock.getType().name().contains("BEDROCK")) {
                return y;
            }
        }
        return -1;
    }
}