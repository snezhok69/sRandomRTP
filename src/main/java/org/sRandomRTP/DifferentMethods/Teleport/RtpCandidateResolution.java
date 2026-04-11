package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;

public final class RtpCandidateResolution {

    private final World world;
    private final Chunk chunk;
    private final int x;
    private final int y;
    private final int z;
    private final int chunkX;
    private final int chunkZ;
    private final Biome biome;
    private final Material surfaceBlockType;
    private final Material feetBlockType;
    private final Material headBlockType;
    private final int skyLight;
    private final boolean safe;
    private final boolean alreadyLoaded;
    private final boolean generationAllowed;
    private final long chunkAcquireNanos;
    private final long safeSearchNanos;

    public RtpCandidateResolution(World world,
                                  Chunk chunk,
                                  int x,
                                  int y,
                                  int z,
                                  Biome biome,
                                  Material surfaceBlockType,
                                  Material feetBlockType,
                                  Material headBlockType,
                                  int skyLight,
                                  boolean safe,
                                  boolean alreadyLoaded,
                                  boolean generationAllowed,
                                  long chunkAcquireNanos,
                                  long safeSearchNanos) {
        this.world = world;
        this.chunk = chunk;
        this.x = x;
        this.y = y;
        this.z = z;
        this.chunkX = x >> 4;
        this.chunkZ = z >> 4;
        this.biome = biome;
        this.surfaceBlockType = surfaceBlockType;
        this.feetBlockType = feetBlockType;
        this.headBlockType = headBlockType;
        this.skyLight = skyLight;
        this.safe = safe;
        this.alreadyLoaded = alreadyLoaded;
        this.generationAllowed = generationAllowed;
        this.chunkAcquireNanos = chunkAcquireNanos;
        this.safeSearchNanos = safeSearchNanos;
    }

    public static RtpCandidateResolution unsafe(World world, Chunk chunk, int x, int z,
                                                boolean alreadyLoaded, boolean generationAllowed,
                                                long chunkAcquireNanos, long safeSearchNanos) {
        return new RtpCandidateResolution(world, chunk, x, -1, z, null,
                null, null, null, 0, false, alreadyLoaded, generationAllowed,
                chunkAcquireNanos, safeSearchNanos);
    }

    public World getWorld() {
        return world;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    public Biome getBiome() {
        return biome;
    }

    public Material getSurfaceBlockType() {
        return surfaceBlockType;
    }

    public Material getFeetBlockType() {
        return feetBlockType;
    }

    public Material getHeadBlockType() {
        return headBlockType;
    }

    public int getSkyLight() {
        return skyLight;
    }

    public boolean isSafe() {
        return safe;
    }

    public boolean isAlreadyLoaded() {
        return alreadyLoaded;
    }

    public boolean isGenerationAllowed() {
        return generationAllowed;
    }

    public long getChunkAcquireNanos() {
        return chunkAcquireNanos;
    }

    public long getSafeSearchNanos() {
        return safeSearchNanos;
    }

    public boolean isChunkReadyForSynchronousTeleport() {
        return chunk != null && chunk.isLoaded() && world != null;
    }

    public Location toLocation() {
        return world == null ? null : new Location(world, x + 0.5D, y, z + 0.5D);
    }
}
