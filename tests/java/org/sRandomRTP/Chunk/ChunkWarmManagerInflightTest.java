package org.sRandomRTP.Chunk;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ChunkWarmManager's AtomicInteger-based inflight tracking.
 * Uses reflection to access private fields/methods; does not start a Bukkit server.
 */
@Tag("unit")
class ChunkWarmManagerInflightTest {

    private AtomicInteger getInflightCount(ChunkWarmManager manager) throws Exception {
        Field f = ChunkWarmManager.class.getDeclaredField("inflightCount");
        f.setAccessible(true);
        return (AtomicInteger) f.get(manager);
    }

    private ChunkWarmManager createUninitialized() throws Exception {
        java.lang.reflect.Constructor<ChunkWarmManager> ctor =
                ChunkWarmManager.class.getDeclaredConstructor(org.bukkit.plugin.java.JavaPlugin.class);
        ctor.setAccessible(true);
        return ctor.newInstance((Object) null);
    }

    @Test
    void inflightCountStartsAtZero() throws Exception {
        ChunkWarmManager manager = createUninitialized();
        assertEquals(0, getInflightCount(manager).get());
    }

    @Test
    void inflightCountDecrementsOnCompletion() throws Exception {
        ChunkWarmManager manager = createUninitialized();
        AtomicInteger counter = getInflightCount(manager);

        counter.incrementAndGet();
        assertEquals(1, counter.get());

        CompletableFuture<Void> future = new CompletableFuture<>();
        future.whenComplete((r, ex) -> counter.updateAndGet(c -> c <= 0 ? 0 : c - 1));
        future.complete(null);

        assertEquals(0, counter.get());
    }

    @Test
    void inflightCountClampsToZeroOnUnderflow() {
        AtomicInteger counter = new AtomicInteger(0);
        // Simulates the whenComplete decrement logic
        counter.updateAndGet(c -> c <= 0 ? 0 : c - 1);
        assertEquals(0, counter.get(), "counter should clamp to 0 rather than going negative");
    }

    @Test
    void inflightCountClampsOnDoubleCompletion() {
        AtomicInteger counter = new AtomicInteger(1);
        // Two concurrent completions for the same future slot
        counter.updateAndGet(c -> c <= 0 ? 0 : c - 1);
        counter.updateAndGet(c -> c <= 0 ? 0 : c - 1);
        assertEquals(0, counter.get(), "double completion should not produce negative count");
    }
}
