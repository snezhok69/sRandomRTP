package org.sRandomRTP.BlockBiomes;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.sRandomRTP.DifferentMethods.Variables;

public class BiomeBlockValidator {

    public static boolean isBiomeBanned(Biome biome) {
        if (biome == null) return false;
        if (Variables.bannedBiomesEnumSet.contains(biome)) return true;
        if (Variables.blockCaveBiomes && Variables.caveBiomesEnumSet.contains(biome)) return true;
        if (Variables.blockOceanRiverBiomes && Variables.oceanRiverBiomesEnumSet.contains(biome)) return true;
        return false;
    }

    public static boolean isBlockBanned(Material material) {
        if (material == null) return false;
        return Variables.blockList.contains(material);
    }
}
