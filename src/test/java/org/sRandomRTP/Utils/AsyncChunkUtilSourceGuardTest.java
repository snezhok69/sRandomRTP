package org.sRandomRTP.Utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncChunkUtilSourceGuardTest {

    /**
     * On Paper servers, the RTP critical path intentionally uses
     * {@code PaperLib.getChunkAtAsyncUrgently} when chunk generation is requested
     * so that the chunk worker prioritises new-chunk loads over background pre-generation.
     * This test documents and guards that intentional choice.
     */
    @Test
    void asyncChunkUtilUsesUrgentLoadingOnPaperForGeneratePath() throws IOException {
        String content = new String(Files.readAllBytes(
                Paths.get("src/main/java/org/sRandomRTP/Utils/AsyncChunkUtil.java")), StandardCharsets.UTF_8);

        assertTrue(content.contains("getChunkAtAsyncUrgently"),
                "Paper RTP critical path should use urgent chunk loading when generate=true.");
    }

    @Test
    void asyncChunkUtilFallsBackToNonUrgentWhenGenerateIsFalse() throws IOException {
        String content = new String(Files.readAllBytes(
                Paths.get("src/main/java/org/sRandomRTP/Utils/AsyncChunkUtil.java")), StandardCharsets.UTF_8);

        assertTrue(content.contains("getChunkAtAsync("),
                "Non-generate path should still use non-urgent PaperLib.getChunkAtAsync.");
    }
}
