package org.sRandomRTP.Commands;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandRtpBiomeTest {

    @Test
    void tokenizesBiomeArgumentsAcrossSpacesAndCommas() {
        List<String> tokens = CommandRtpBiome.tokenizeBiomeArguments(new String[]{
                "desert, savanna",
                "badlands",
                "cherry grove"
        });

        assertEquals(Arrays.asList("desert", "savanna", "badlands", "cherry grove"), tokens);
    }

    @Test
    void normalizesBiomeNamesWithCaseHyphensAndSpaces() {
        assertEquals("CHERRY_GROVE", CommandRtpBiome.normalizeBiomeToken("Cherry-Grove"));
        assertEquals("OLD_GROWTH_PINE_TAIGA", CommandRtpBiome.normalizeBiomeToken("old growth pine taiga"));
    }
}
