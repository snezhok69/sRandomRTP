package org.sRandomRTP.DifferentMethods.Text;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TranslateRGBColorsTest {

    @Test
    void nullInputReturnsNull() {
        assertNull(TranslateRGBColors.translateRGBColors(null));
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertEquals("", TranslateRGBColors.translateRGBColors(""));
    }

    @Test
    void noRgbCodeLeavesStringUnchanged() {
        String input = "Hello &aworld";
        // No &#XXXXXX patterns — only &a colour code remains (translated by Bukkit ChatColor)
        String result = TranslateRGBColors.translateRGBColors(input);
        assertNotNull(result);
        assertFalse(result.contains("&#"), "Output should not contain untranslated &#codes");
    }

    @Test
    void singleRgbCodeIsReplaced() {
        String input = "&#ff0000Red text";
        String result = TranslateRGBColors.translateRGBColors(input);
        assertNotNull(result);
        assertFalse(result.contains("&#ff0000"), "RGB code should be replaced");
        // BungeeCord ChatColor produces §x§f§f§0§0§0§0 for #ff0000
        assertTrue(result.contains("\u00a7x"), "Result should contain §x (RGB colour marker)");
    }

    @Test
    void multipleRgbCodesAreAllReplaced() {
        // This is the primary regression test: the old O(N²) loop broke on multiple codes
        // because it rebuilt a new Matcher after each replace(), losing subsequent matches.
        String input = "&#ff0000Red &#00ff00Green &#0000ffBlue";
        String result = TranslateRGBColors.translateRGBColors(input);
        assertNotNull(result);
        assertFalse(result.contains("&#ff0000"), "First RGB code should be replaced");
        assertFalse(result.contains("&#00ff00"), "Second RGB code should be replaced");
        assertFalse(result.contains("&#0000ff"), "Third RGB code should be replaced");
        // All three §x markers must appear in the output
        long sxCount = result.chars().filter(c -> c == '\u00a7').count();
        // Each RGB colour is §x§R§R§G§G§B§B = 7 section signs; 3 colours = 21+ section signs
        assertTrue(sxCount >= 3, "All three RGB colours must be translated (found " + sxCount + " § chars)");
    }

    @Test
    void regularColourCodesAreTranslated() {
        String input = "&aGreen &bAqua";
        String result = TranslateRGBColors.translateRGBColors(input);
        assertNotNull(result);
        assertTrue(result.contains("\u00a7a"), "§a should be present");
        assertTrue(result.contains("\u00a7b"), "§b should be present");
    }
}
