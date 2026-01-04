package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.List;

public class IsBiomeBanned {
    public static boolean isBiomeBanned(Biome biome) {
        if (biome == null || Variables.teleportfile == null) {
            return false;
        }

        List<String> bannedBiomesList = Variables.teleportfile.getStringList("teleport.bannedBiomes");
        if (bannedBiomesList.contains(biome.name())) {
            return true;
        }

        boolean blockCaveBiomes = Variables.teleportfile.getBoolean("teleport.block-cave-biomes", true);
        if (blockCaveBiomes && biome.name().contains("CAVES")) {
            return true;
        }

        boolean blockOceanRiverBiomes = Variables.teleportfile.getBoolean("teleport.block-ocean-river-biomes", true);
        if (blockOceanRiverBiomes && (biome.name().contains("OCEAN") || biome.name().contains("RIVER"))) {
            return true;
        }
        return false;
    }
}