package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.sRandomRTP.Utils.AsyncChunkUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ChunkAcquireService {

    private ChunkAcquireService() {
    }

    public static CompletableFuture<ChunkAcquireResult> acquireTargetChunk(World world, int x, int z, boolean preferGeneratedOnly) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;
        final boolean alreadyLoaded = world.isChunkLoaded(chunkX, chunkZ);
        final boolean generationAllowed = !preferGeneratedOnly;
        final long startedAt = System.nanoTime();

        return AsyncChunkUtil.requestChunk(world, chunkX, chunkZ, generationAllowed)
                .handle((chunk, throwable) -> {
                    long duration = System.nanoTime() - startedAt;
                    if (throwable != null || chunk == null || !chunk.isLoaded()) {
                        return new ChunkAcquireResult(world, chunkX, chunkZ, null, alreadyLoaded, generationAllowed, duration);
                    }
                    return new ChunkAcquireResult(world, chunkX, chunkZ, chunk, alreadyLoaded, generationAllowed, duration);
                });
    }

    public static List<CompletableFuture<Chunk>> preloadChunksAround(Location location, int radius) {
        if (location == null || location.getWorld() == null || radius <= 0) {
            return Collections.emptyList();
        }

        World world = location.getWorld();
        int centerChunkX = location.getBlockX() >> 4;
        int centerChunkZ = location.getBlockZ() >> 4;
        int effectiveRadius = Math.max(0, radius);
        if (effectiveRadius == 0) {
            return Collections.emptyList();
        }

        int capacity = (2 * effectiveRadius + 1) * (2 * effectiveRadius + 1) - 1;
        List<CompletableFuture<Chunk>> futures = new ArrayList<>(capacity);
        for (int dx = -effectiveRadius; dx <= effectiveRadius; dx++) {
            for (int dz = -effectiveRadius; dz <= effectiveRadius; dz++) {
                if (dx == 0 && dz == 0) continue; // center chunk already loaded by acquireTargetChunk
                futures.add(AsyncChunkUtil.requestChunk(world, centerChunkX + dx, centerChunkZ + dz, false));
            }
        }
        return futures;
    }

    public static final class ChunkAcquireResult {
        private final World world;
        private final int chunkX;
        private final int chunkZ;
        private final Chunk chunk;
        private final boolean alreadyLoaded;
        private final boolean generationAllowed;
        private final long durationNanos;

        public ChunkAcquireResult(World world, int chunkX, int chunkZ, Chunk chunk,
                                  boolean alreadyLoaded, boolean generationAllowed, long durationNanos) {
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.chunk = chunk;
            this.alreadyLoaded = alreadyLoaded;
            this.generationAllowed = generationAllowed;
            this.durationNanos = durationNanos;
        }

        public World getWorld() {
            return world;
        }

        public int getChunkX() {
            return chunkX;
        }

        public int getChunkZ() {
            return chunkZ;
        }

        public Chunk getChunk() {
            return chunk;
        }

        public boolean isAlreadyLoaded() {
            return alreadyLoaded;
        }

        public boolean isGenerationAllowed() {
            return generationAllowed;
        }

        public long getDurationNanos() {
            return durationNanos;
        }

        public boolean isReady() {
            return chunk != null && chunk.isLoaded();
        }
    }
}
