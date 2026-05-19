package org.sRandomRTP.BlockBiomes;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BiomeBlockValidator}.
 * Does not require a running Bukkit server (no MockBukkit).
 */
@Tag("unit")
class BiomeBlockValidatorTest {

    private BiomeFilterSnapshot previous;

    @BeforeEach
    void setUp() {
        previous = Variables.biomeFilters;
    }

    @AfterEach
    void tearDown() {
        Variables.biomeFilters = previous;
    }

    // ── B8 regression ────────────────────────────────────────────────────────

    @Test
    void nullBiomeReturnsTrueBanned() {
        // B8 fix: null biome must be treated as banned to prevent teleport to unknown location
        assertTrue(BiomeBlockValidator.isBiomeBanned(null),
                "null biome must be treated as banned (true) not allowed");
    }

    // ── Empty snapshot (no banned biomes, cave/ocean filtering enabled) ──────

    @Test
    void emptyCaveBiomesSnapshotAllowsForest() {
        // Snapshot with empty caves/ocean sets but blockCave=true still allows any non-cave biome
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.<String>emptySet(), Collections.<Biome>emptySet(),
                Collections.<Biome>emptySet(), Collections.<Biome>emptySet(), true, true);
        // FOREST is not in an empty cave or ocean set — must be allowed
        assertFalse(BiomeBlockValidator.isBiomeBanned(Biome.FOREST),
                "FOREST must not be banned when no biomes are explicitly banned");
    }

    // ── Explicitly banned biome ───────────────────────────────────────────────

    @Test
    void explicitlyBannedBiomeReturnsBanned() {
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.singleton("FOREST"), Collections.singleton(Biome.FOREST),
                Collections.<Biome>emptySet(), Collections.<Biome>emptySet(), false, false);
        assertTrue(BiomeBlockValidator.isBiomeBanned(Biome.FOREST),
                "FOREST must be banned when present in bannedBiomes set");
    }

    // ── Cave biome flag ───────────────────────────────────────────────────────

    @Test
    void caveBiomeBannedWhenFlagEnabled() {
        // Use a known cave biome if available; fall back to FOREST as placeholder
        Biome cave = findBiomeContaining("CAVES");
        if (cave == null) {
            return; // No cave biome available in this Bukkit version — skip
        }
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.<String>emptySet(), Collections.<Biome>emptySet(),
                Collections.singleton(cave), Collections.<Biome>emptySet(), true, false);
        assertTrue(BiomeBlockValidator.isBiomeBanned(cave),
                "Cave biome must be banned when blockCave=true");
    }

    @Test
    void caveBiomeAllowedWhenFlagDisabled() {
        Biome cave = findBiomeContaining("CAVES");
        if (cave == null) return;
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.<String>emptySet(), Collections.<Biome>emptySet(),
                Collections.singleton(cave), Collections.<Biome>emptySet(), false, false);
        assertFalse(BiomeBlockValidator.isBiomeBanned(cave),
                "Cave biome must be allowed when blockCave=false");
    }

    // ── Ocean/river biome flag ────────────────────────────────────────────────

    @Test
    void oceanBiomeBannedWhenFlagEnabled() {
        Biome ocean = findBiomeContaining("OCEAN");
        if (ocean == null) return;
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.<String>emptySet(), Collections.<Biome>emptySet(),
                Collections.<Biome>emptySet(), Collections.singleton(ocean), false, true);
        assertTrue(BiomeBlockValidator.isBiomeBanned(ocean),
                "Ocean biome must be banned when blockOceanRiver=true");
    }

    @Test
    void oceanBiomeAllowedWhenFlagDisabled() {
        Biome ocean = findBiomeContaining("OCEAN");
        if (ocean == null) return;
        Variables.biomeFilters = new BiomeFilterSnapshot(
                Collections.<String>emptySet(), Collections.<Biome>emptySet(),
                Collections.<Biome>emptySet(), Collections.singleton(ocean), false, false);
        assertFalse(BiomeBlockValidator.isBiomeBanned(ocean),
                "Ocean biome must be allowed when blockOceanRiver=false");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Biome findBiomeContaining(String substring) {
        try {
            for (Biome b : Biome.values()) {
                if (b.name().contains(substring)) return b;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
