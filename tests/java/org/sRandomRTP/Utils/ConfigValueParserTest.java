package org.sRandomRTP.Utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigValueParserTest {

    @Test
    void parsesPotionEffectsByLegacyIdAndReadableNames() {
        assertEquals(PotionEffectType.DAMAGE_RESISTANCE, ConfigValueParser.parsePotionEffectType("11"));
        assertEquals(PotionEffectType.DAMAGE_RESISTANCE, ConfigValueParser.parsePotionEffectType("resistance"));
        assertEquals(PotionEffectType.JUMP, ConfigValueParser.parsePotionEffectType("jump boost"));
        assertEquals(PotionEffectType.SPEED, ConfigValueParser.parsePotionEffectType("minecraft:speed"));
    }

    @Test
    void parsesParticlesByNameNamespaceAndEnumNumber() {
        assertEquals(Particle.FLAME, ConfigValueParser.parseParticle("flame"));
        assertEquals(Particle.FLAME, ConfigValueParser.parseParticle("minecraft:flame"));
        assertEquals(Particle.FLAME, ConfigValueParser.parseParticle(String.valueOf(Particle.FLAME.ordinal())));
    }

    @Test
    void parsesSoundsAndBossBarEnumsWithFlexibleText() {
        assertEquals(Sound.BLOCK_NOTE_BLOCK_BIT, ConfigValueParser.parseSound("block note block bit"));
        assertEquals(Sound.BLOCK_NOTE_BLOCK_BIT, ConfigValueParser.parseSound("minecraft:block_note_block_bit"));
        assertEquals(BarColor.BLUE, ConfigValueParser.parseBarColor("blue", BarColor.WHITE));
        assertEquals(BarStyle.SEGMENTED_10, ConfigValueParser.parseBarStyle("segmented 10", BarStyle.SOLID));
    }

    @Test
    void parsesMaterialsAndBiomesWithReadableText() {
        assertEquals(Material.GRASS_BLOCK, ConfigValueParser.parseMaterial("grass block"));
        assertEquals(Material.GLASS, ConfigValueParser.parseMaterial("minecraft:glass"));
        assertEquals(Biome.SWAMP, ConfigValueParser.parseBiome("minecraft:swamp"));
    }

    @Test
    void returnsNullForUnknownValues() {
        assertNull(ConfigValueParser.parseParticle("definitely_missing_particle"));
        assertNull(ConfigValueParser.parsePotionEffectType("99999"));
    }
}
