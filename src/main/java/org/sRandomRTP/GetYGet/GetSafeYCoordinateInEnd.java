package org.sRandomRTP.GetYGet;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class GetSafeYCoordinateInEnd {
    public static int getSafeYCoordinateInEnd(World world, int x, int z) {
        for (int y = world.getMaxHeight() - 1; y > 0; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block belowBlock = block.getRelative(BlockFace.DOWN);
            Block twoBelowBlock = belowBlock.getRelative(BlockFace.DOWN);
            if (block.getType() == Material.AIR && belowBlock.getType().isSolid() && !belowBlock.getType().name().contains("AIR")
                    && twoBelowBlock.getType().isSolid() && !twoBelowBlock.getType().name().contains("AIR")) {
                return y;
            }
        }
        return -1;
    }
}