package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.logging.Level;

public class BiomeBlockValidator {

    public static boolean isBiomeBanned(Biome biome) {
        if (biome == null) {
            if (Variables.getInstance() != null) {
                Variables.getInstance().getLogger().log(Level.WARNING,
                        "[sRandomRTP] isBiomeBanned called with null biome — treating as banned to prevent teleport to unknown location");
            }
            return true;
        }
        BiomeFilterSnapshot f = Variables.biomeFilters;
        if (f.bannedBiomes().contains(biome)) return true;
        if (f.blockCave() && f.caveBiomes().contains(biome)) return true;
        if (f.blockOceanRiver() && f.oceanRiverBiomes().contains(biome)) return true;
        return false;
    }

    public static boolean isBlockBanned(Material material) {
        if (material == null) return false;
        return Variables.blockList.contains(material);
    }
}
