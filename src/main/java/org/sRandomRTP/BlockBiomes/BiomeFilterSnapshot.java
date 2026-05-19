package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;

import java.util.Collections;
import java.util.Set;

/**
 * Immutable, atomically-replaceable snapshot of biome filter state.
 * Written once in {@link LoadBlockList#loadAllCaches()} and read
 * lock-free by {@link BiomeBlockValidator}.
 */
public final class BiomeFilterSnapshot {
    /** Empty snapshot used before the first config load. */
    public static final BiomeFilterSnapshot EMPTY = new BiomeFilterSnapshot(
            Collections.<String>emptySet(), Collections.<Biome>emptySet(),
            Collections.<Biome>emptySet(), Collections.<Biome>emptySet(), true, true
    );

    private final Set<String> bannedBiomesNames;
    private final Set<Biome> bannedBiomes;
    private final Set<Biome> caveBiomes;
    private final Set<Biome> oceanRiverBiomes;
    private final boolean blockCave;
    private final boolean blockOceanRiver;

    public BiomeFilterSnapshot(Set<String> bannedBiomesNames,
                               Set<Biome> bannedBiomes,
                               Set<Biome> caveBiomes,
                               Set<Biome> oceanRiverBiomes,
                               boolean blockCave,
                               boolean blockOceanRiver) {
        this.bannedBiomesNames = bannedBiomesNames == null ? Collections.<String>emptySet() : bannedBiomesNames;
        this.bannedBiomes = bannedBiomes == null ? Collections.<Biome>emptySet() : bannedBiomes;
        this.caveBiomes = caveBiomes == null ? Collections.<Biome>emptySet() : caveBiomes;
        this.oceanRiverBiomes = oceanRiverBiomes == null ? Collections.<Biome>emptySet() : oceanRiverBiomes;
        this.blockCave = blockCave;
        this.blockOceanRiver = blockOceanRiver;
    }

    public Set<String> bannedBiomesNames() {
        return bannedBiomesNames;
    }

    public Set<Biome> bannedBiomes() {
        return bannedBiomes;
    }

    public Set<Biome> caveBiomes() {
        return caveBiomes;
    }

    public Set<Biome> oceanRiverBiomes() {
        return oceanRiverBiomes;
    }

    public boolean blockCave() {
        return blockCave;
    }

    public boolean blockOceanRiver() {
        return blockOceanRiver;
    }
}
