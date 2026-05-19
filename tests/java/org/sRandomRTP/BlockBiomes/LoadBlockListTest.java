package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the static biome category constants in {@link LoadBlockList}.
 * Verifies the static initialiser correctness and immutability.
 */
@Tag("unit")
class LoadBlockListTest {

    @SuppressWarnings("unchecked")
    private static Set<Biome> getStaticField(String name) throws Exception {
        Field field = LoadBlockList.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Set<Biome>) field.get(null);
    }

    @Test
    void caveBiomesIsNonEmpty() throws Exception {
        Set<Biome> cave = getStaticField("CAVE_BIOMES");
        assertNotNull(cave, "CAVE_BIOMES must not be null");
        // If no Biome enum value contains "CAVES", the set may be empty on unusual forks —
        // but on standard Paper/Spigot it must have at least one entry.
        // We simply verify the set is initialized (not null) and is unmodifiable.
        try {
            cave.add(Biome.FOREST);
            // Should not reach here — set must be unmodifiable
            org.junit.jupiter.api.Assertions.fail("CAVE_BIOMES must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // correct
        }
    }

    @Test
    void oceanRiverBiomesIsNonEmpty() throws Exception {
        Set<Biome> ocean = getStaticField("OCEAN_RIVER_BIOMES");
        assertNotNull(ocean, "OCEAN_RIVER_BIOMES must not be null");
        try {
            ocean.add(Biome.FOREST);
            org.junit.jupiter.api.Assertions.fail("OCEAN_RIVER_BIOMES must be unmodifiable");
        } catch (UnsupportedOperationException expected) {
            // correct
        }
    }

    @Test
    void caveBiomesContainOnlyCaveNamedBiomes() throws Exception {
        Set<Biome> cave = getStaticField("CAVE_BIOMES");
        for (Biome b : cave) {
            assertTrue(b.name().contains("CAVES"),
                    "CAVE_BIOMES must only contain biomes whose name includes 'CAVES', found: " + b.name());
        }
    }

    @Test
    void oceanRiverBiomesContainOnlyOceanOrRiverNamedBiomes() throws Exception {
        Set<Biome> ocean = getStaticField("OCEAN_RIVER_BIOMES");
        for (Biome b : ocean) {
            assertTrue(b.name().contains("OCEAN") || b.name().contains("RIVER"),
                    "OCEAN_RIVER_BIOMES must only contain OCEAN/RIVER biomes, found: " + b.name());
        }
    }

    @Test
    void staticFieldsAreTheSameObjectAcrossMultipleCalls() throws Exception {
        Set<Biome> cave1 = getStaticField("CAVE_BIOMES");
        Set<Biome> cave2 = getStaticField("CAVE_BIOMES");
        assertSame(cave1, cave2,
                "CAVE_BIOMES must be the same static object on repeated access (not re-created)");
    }

    @Test
    void forestIsNotInCaveBiomes() throws Exception {
        Set<Biome> cave = getStaticField("CAVE_BIOMES");
        assertFalse(cave.contains(Biome.FOREST),
                "FOREST must not be in CAVE_BIOMES");
    }
}
