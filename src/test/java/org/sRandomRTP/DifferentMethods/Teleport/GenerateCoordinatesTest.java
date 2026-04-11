package org.sRandomRTP.DifferentMethods.Teleport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sRandomRTP.DifferentMethods.Variables;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCoordinatesTest {

    @BeforeEach
    void setUp() {
        Variables.instance = null;
    }

    @Test
    void deterministicGenerationReturnsSameCoordinatesForSameSeed() {
        int[] first = GenerateCoordinates.generateCoordinates(
                "Steve", 3, 42L, 100, 200, 1500, 200, "CIRCLE", false);
        int[] second = GenerateCoordinates.generateCoordinates(
                "Steve", 3, 42L, 100, 200, 1500, 200, "CIRCLE", false);

        assertArrayEquals(first, second);
    }

    @Test
    void generatedCoordinatesStayInsideWorldLimit() {
        int[] generated = GenerateCoordinates.generateCoordinates(
                "Alex", 0, 99L, 29_999_980, 29_999_980, 10_000, 0, "SQUARE", true);

        assertTrue(generated[0] <= 29_999_984 && generated[0] >= -29_999_984);
        assertTrue(generated[1] <= 29_999_984 && generated[1] >= -29_999_984);
    }
}
