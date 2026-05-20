package org.sRandomRTP.Files;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TeleportConfigLayoutTest {

    @Test
    void bannedBlocksStayNextToBannedBiomesInDefaultConfigs() throws Exception {
        assertBannedBlocksNearBiomes("Settings/teleport.yml");
        assertBannedBlocksNearBiomes("Settings_ru/teleport.yml");
    }

    private void assertBannedBlocksNearBiomes(String resourcePath) throws Exception {
        InputStream input = TeleportConfigLayoutTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assertTrue(input != null, "Missing resource: " + resourcePath);
        String content = new String(readAll(input), StandardCharsets.UTF_8);

        int biomes = content.indexOf("bannedBiomes:");
        int blocks = content.indexOf("bannedBlocks:");
        int minY = content.indexOf("minY:");

        assertTrue(biomes >= 0, resourcePath + " must contain bannedBiomes");
        assertTrue(blocks > biomes, resourcePath + " must place bannedBlocks after bannedBiomes");
        assertTrue(minY > blocks, resourcePath + " must place minY after bannedBlocks");
    }

    private byte[] readAll(InputStream input) throws Exception {
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}
