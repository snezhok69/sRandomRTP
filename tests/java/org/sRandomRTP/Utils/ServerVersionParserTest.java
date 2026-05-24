package org.sRandomRTP.Utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerVersionParserTest {

    @Test
    void parsesLegacyOneDotVersionsAsMinorCompatibilityNumber() {
        assertEquals(16, ServerVersionParser.parseCompatibilityNumber("1.16.5"));
        assertEquals(21, ServerVersionParser.parseCompatibilityNumber("1.21.10-R0.1-SNAPSHOT"));
    }

    @Test
    void parsesNewCalendarVersionsAsMajorCompatibilityNumber() {
        assertEquals(26, ServerVersionParser.parseCompatibilityNumber("26.1"));
        assertEquals(26, ServerVersionParser.parseCompatibilityNumber("26.1.1"));
        assertEquals(26, ServerVersionParser.parseCompatibilityNumber("26.1.2-R0.1-SNAPSHOT"));
    }

    @Test
    void parsesCraftBukkitPackageFragmentsForBothVersionSchemes() {
        assertEquals(20, ServerVersionParser.parseCraftBukkitPackageCompatibilityNumber("v1_20_R3"));
        assertEquals(26, ServerVersionParser.parseCraftBukkitPackageCompatibilityNumber("v26_1_R1"));
    }

    @Test
    void rejectsInvalidVersions() {
        assertEquals(-1, ServerVersionParser.parseCompatibilityNumber(""));
        assertEquals(-1, ServerVersionParser.parseCompatibilityNumber("not-a-version"));
        assertEquals(-1, ServerVersionParser.parseCraftBukkitPackageCompatibilityNumber("version"));
    }
}
