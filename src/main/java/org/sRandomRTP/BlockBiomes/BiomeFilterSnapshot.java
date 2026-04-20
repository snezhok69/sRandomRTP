package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;

import java.util.Set;

/**
 * Immutable, atomically-replaceable snapshot of biome filter state.
 * Written once in {@link LoadBlockList#loadAllCaches()} and read
 * lock-free by {@link BiomeBlockValidator}.
 */
public record BiomeFilterSnapshot(
        Set<String> bannedBiomesNames,
        Set<Biome> bannedBiomes,
        Set<Biome> caveBiomes,
        Set<Biome> oceanRiverBiomes,
        boolean blockCave,
        boolean blockOceanRiver
) {
    /** Empty snapshot used before the first config load. */
    public static final BiomeFilterSnapshot EMPTY = new BiomeFilterSnapshot(
            Set.of(), Set.of(), Set.of(), Set.of(), true, true
    );
}
