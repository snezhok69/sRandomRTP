package org.sRandomRTP.Services;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ConfigCacheLoadingTitleTest {

    // ── field presence ────────────────────────────────────────────────────────

    @Test
    void configCache_exposesAllFiveTitleLoadingFields() throws Exception {
        ConfigCache cache = cacheWithDefaults();
        // Assert fields exist AND are accessible by name
        assertNotNull(field(cache, "titleLoadingEnabled"));
        assertNotNull(field(cache, "subtitleLoadingEnabled"));
        assertNotNull(field(cache, "titleFadeInLoading"));
        assertNotNull(field(cache, "titleStayLoading"));
        assertNotNull(field(cache, "titleFadeOutLoading"));
    }

    // ── safe defaults ─────────────────────────────────────────────────────────

    @Test
    void allLoadingTitleFieldsDefaultToFalseOrStandardTicks() {
        ConfigCache cache = cacheWithDefaults();

        assertFalse(cache.titleLoadingEnabled,   "titleLoadingEnabled must default false");
        assertFalse(cache.subtitleLoadingEnabled, "subtitleLoadingEnabled must default false");
        // defaults: 0.5 s × 20 = 10, 3.5 s × 20 = 70, 1.0 s × 20 = 20
        assertEquals(10, cache.titleFadeInLoading,  "titleFadeInLoading default should be 10 ticks");
        assertEquals(70, cache.titleStayLoading,    "titleStayLoading default should be 70 ticks");
        assertEquals(20, cache.titleFadeOutLoading, "titleFadeOutLoading default should be 20 ticks");
    }

    // ── builder round-trip ────────────────────────────────────────────────────

    @Test
    void titleLoadingEnabled_trueFromBuilder() {
        ConfigCache cache = cacheFrom(b -> b.titleLoadingEnabled = true);
        assertTrue(cache.titleLoadingEnabled);
    }

    @Test
    void subtitleLoadingEnabled_trueFromBuilder() {
        ConfigCache cache = cacheFrom(b -> b.subtitleLoadingEnabled = true);
        assertTrue(cache.subtitleLoadingEnabled);
    }

    @Test
    void titleFadeInLoading_customTicksFromBuilder() {
        // 0.5 s * 20 = 10
        ConfigCache cache = cacheFrom(b -> b.titleFadeInLoading = 10);
        assertEquals(10, cache.titleFadeInLoading);
    }

    @Test
    void titleStayLoading_customTicksFromBuilder() {
        ConfigCache cache = cacheFrom(b -> b.titleStayLoading = 50);
        assertEquals(50, cache.titleStayLoading);
    }

    @Test
    void titleFadeOutLoading_customTicksFromBuilder() {
        ConfigCache cache = cacheFrom(b -> b.titleFadeOutLoading = 15);
        assertEquals(15, cache.titleFadeOutLoading);
    }

    // ── YAML-to-ticks conversion arithmetic ───────────────────────────────────

    @Test
    void halfSecond_convertedTo10Ticks() {
        // 0.5 s × 20 ticks/s = 10
        assertEquals(10, toTicks(0.5));
    }

    @Test
    void threeAndHalfSeconds_convertedTo70Ticks() {
        assertEquals(70, toTicks(3.5));
    }

    @Test
    void oneSecond_convertedTo20Ticks() {
        assertEquals(20, toTicks(1.0));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static int toTicks(double seconds) {
        return (int) (seconds * 20);
    }

    private static ConfigCache cacheWithDefaults() {
        return new ConfigCache(new ConfigCache.Builder());
    }

    private static ConfigCache cacheFrom(java.util.function.Consumer<ConfigCache.Builder> customizer) {
        ConfigCache.Builder b = new ConfigCache.Builder();
        customizer.accept(b);
        return new ConfigCache(b);
    }

    private static Field field(ConfigCache cache, String name) throws Exception {
        Field f = ConfigCache.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
