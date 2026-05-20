package org.sRandomRTP.Services;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerApiGuardTest {

    private static final String[] FORBIDDEN_PATTERNS = {
            "Bukkit.getScheduler",
            "BukkitRunnable",
            ".runTask(",
            ".runTaskLater(",
            ".runTaskTimer(",
            "scheduleSync",
            "scheduleAsync"
    };

    @Test
    void mainSourceDoesNotUseDirectBukkitSchedulers() throws IOException {
        Path sourceRoot = Paths.get("src/main/java");
        List<String> violations = new ArrayList<String>();

        try (Stream<Path> stream = Files.walk(sourceRoot)) {
            stream.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .forEach(path -> collectViolations(path, violations));
        }

        assertTrue(violations.isEmpty(), "Forbidden scheduler API usage detected:\n" + String.join("\n", violations));
    }

    @Test
    void portalAndChunkWarmTasksDoNotUseAsyncTimersForBukkitApi() throws IOException {
        String[] guardedFiles = {
                "src/main/java/org/sRandomRTP/Chunk/ChunkWarmManager.java",
                "src/main/java/org/sRandomRTP/Commands/CommandSetPortal.java",
                "src/main/java/org/sRandomRTP/DataPortals/PortalSQLRepository.java",
                "src/main/java/org/sRandomRTP/Commands/CommandReload.java"
        };
        List<String> violations = new ArrayList<String>();
        for (String guardedFile : guardedFiles) {
            Path path = Paths.get(guardedFile);
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            if (content.contains(".runTimerAsync(")) {
                violations.add(guardedFile + " -> .runTimerAsync(");
            }
        }
        assertTrue(violations.isEmpty(), "Unsafe async timer usage detected:\n" + String.join("\n", violations));
    }

    private void collectViolations(Path path, List<String> violations) {
        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            for (String pattern : FORBIDDEN_PATTERNS) {
                if (content.contains(pattern)) {
                    violations.add(path.toString() + " -> " + pattern);
                }
            }
        } catch (IOException e) {
            violations.add(path.toString() + " -> read failure: " + e.getMessage());
        }
    }
}
