package org.sRandomRTP.Utils;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Biome;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ConfigValueParser {

    private static final Map<String, String> POTION_ALIASES;
    private static final Map<Integer, String> LEGACY_POTION_IDS;

    static {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("SLOWNESS", "SLOW");
        aliases.put("HASTE", "FAST_DIGGING");
        aliases.put("MINING_FATIGUE", "SLOW_DIGGING");
        aliases.put("STRENGTH", "INCREASE_DAMAGE");
        aliases.put("INSTANT_HEALTH", "HEAL");
        aliases.put("INSTANT_DAMAGE", "HARM");
        aliases.put("JUMP_BOOST", "JUMP");
        aliases.put("NAUSEA", "CONFUSION");
        aliases.put("RESISTANCE", "DAMAGE_RESISTANCE");
        aliases.put("DOLPHIN_GRACE", "DOLPHINS_GRACE");
        POTION_ALIASES = Collections.unmodifiableMap(aliases);

        Map<Integer, String> legacyIds = new HashMap<Integer, String>();
        legacyIds.put(Integer.valueOf(1), "SPEED");
        legacyIds.put(Integer.valueOf(2), "SLOW");
        legacyIds.put(Integer.valueOf(3), "FAST_DIGGING");
        legacyIds.put(Integer.valueOf(4), "SLOW_DIGGING");
        legacyIds.put(Integer.valueOf(5), "INCREASE_DAMAGE");
        legacyIds.put(Integer.valueOf(6), "HEAL");
        legacyIds.put(Integer.valueOf(7), "HARM");
        legacyIds.put(Integer.valueOf(8), "JUMP");
        legacyIds.put(Integer.valueOf(9), "CONFUSION");
        legacyIds.put(Integer.valueOf(10), "REGENERATION");
        legacyIds.put(Integer.valueOf(11), "DAMAGE_RESISTANCE");
        legacyIds.put(Integer.valueOf(12), "FIRE_RESISTANCE");
        legacyIds.put(Integer.valueOf(13), "WATER_BREATHING");
        legacyIds.put(Integer.valueOf(14), "INVISIBILITY");
        legacyIds.put(Integer.valueOf(15), "BLINDNESS");
        legacyIds.put(Integer.valueOf(16), "NIGHT_VISION");
        legacyIds.put(Integer.valueOf(17), "HUNGER");
        legacyIds.put(Integer.valueOf(18), "WEAKNESS");
        legacyIds.put(Integer.valueOf(19), "POISON");
        legacyIds.put(Integer.valueOf(20), "WITHER");
        legacyIds.put(Integer.valueOf(21), "HEALTH_BOOST");
        legacyIds.put(Integer.valueOf(22), "ABSORPTION");
        legacyIds.put(Integer.valueOf(23), "SATURATION");
        legacyIds.put(Integer.valueOf(24), "GLOWING");
        legacyIds.put(Integer.valueOf(25), "LEVITATION");
        legacyIds.put(Integer.valueOf(26), "LUCK");
        legacyIds.put(Integer.valueOf(27), "UNLUCK");
        legacyIds.put(Integer.valueOf(28), "SLOW_FALLING");
        legacyIds.put(Integer.valueOf(29), "CONDUIT_POWER");
        legacyIds.put(Integer.valueOf(30), "DOLPHINS_GRACE");
        legacyIds.put(Integer.valueOf(31), "BAD_OMEN");
        legacyIds.put(Integer.valueOf(32), "HERO_OF_THE_VILLAGE");
        legacyIds.put(Integer.valueOf(33), "DARKNESS");
        LEGACY_POTION_IDS = Collections.unmodifiableMap(legacyIds);
    }

    private ConfigValueParser() {
    }

    public static Particle parseParticle(String raw) {
        return parseEnum(Particle.class, raw);
    }

    public static Sound parseSound(String raw) {
        return parseEnum(Sound.class, raw);
    }

    public static BarColor parseBarColor(String raw, BarColor fallback) {
        BarColor parsed = parseEnum(BarColor.class, raw);
        return parsed == null ? fallback : parsed;
    }

    public static BarStyle parseBarStyle(String raw, BarStyle fallback) {
        BarStyle parsed = parseEnum(BarStyle.class, raw);
        return parsed == null ? fallback : parsed;
    }

    public static Biome parseBiome(String raw) {
        return parseEnum(Biome.class, raw);
    }

    public static Material parseMaterial(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        String normalized = normalizeToken(value);
        Integer numeric = parseInteger(value);
        if (numeric != null) {
            Material legacyMaterial = safeMatchMaterial(value);
            return legacyMaterial == null ? parseEnum(Material.class, normalized) : legacyMaterial;
        }

        Material material = parseEnum(Material.class, normalized);
        if (material != null) {
            return material;
        }

        material = safeMatchMaterial(value);
        if (material != null) {
            return material;
        }
        return safeMatchMaterial(normalized);
    }

    public static PotionEffectType parsePotionEffectType(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        Integer numeric = parseInteger(value);
        if (numeric != null) {
            String legacyName = LEGACY_POTION_IDS.get(numeric);
            if (legacyName != null) {
                PotionEffectType legacyType = potionByName(legacyName);
                if (legacyType != null) {
                    return legacyType;
                }
            }
            try {
                PotionEffectType byId = PotionEffectType.getById(numeric.intValue());
                if (byId != null) {
                    return byId;
                }
            } catch (RuntimeException ignored) {
                // Some server/API combinations do not expose legacy numeric IDs.
            }
            PotionEffectType[] values = PotionEffectType.values();
            return numeric.intValue() >= 0 && numeric.intValue() < values.length ? values[numeric.intValue()] : null;
        }

        String normalized = normalizeToken(value);
        PotionEffectType effectType = potionByName(normalized);
        if (effectType != null) {
            return effectType;
        }

        String alias = POTION_ALIASES.get(normalized);
        return alias == null ? null : potionByName(alias);
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String raw) {
        if (type == null || raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }

        E[] constants = type.getEnumConstants();
        Integer numeric = parseInteger(value);
        if (numeric != null) {
            return numeric.intValue() >= 0 && numeric.intValue() < constants.length ? constants[numeric.intValue()] : null;
        }

        String normalized = normalizeToken(value);
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static String normalizeToken(String raw) {
        String value = raw == null ? "" : raw.trim();
        int namespaceIndex = value.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < value.length()) {
            value = value.substring(namespaceIndex + 1);
        }
        return value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .replace('.', '_')
                .toUpperCase(Locale.ROOT);
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Material safeMatchMaterial(String value) {
        try {
            return Material.matchMaterial(value, true);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static PotionEffectType potionByName(String name) {
        if (name == null) {
            return null;
        }
        PotionEffectType byName = PotionEffectType.getByName(name);
        if (byName != null) {
            return byName;
        }
        try {
            Object value = PotionEffectType.class.getField(name).get(null);
            return value instanceof PotionEffectType ? (PotionEffectType) value : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
