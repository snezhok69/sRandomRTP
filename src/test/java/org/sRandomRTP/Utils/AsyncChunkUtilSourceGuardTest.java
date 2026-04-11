package org.sRandomRTP.Utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class AsyncChunkUtilSourceGuardTest {

    @Test
    void asyncChunkUtilDoesNotUseUrgentChunkLoading() throws IOException {
        String content = new String(Files.readAllBytes(
                Paths.get("src/main/java/org/sRandomRTP/Utils/AsyncChunkUtil.java")), StandardCharsets.UTF_8);

        assertFalse(content.contains("getChunkAtAsyncUrgently"),
                "Search path should not use urgent chunk loading by default.");
    }
}
