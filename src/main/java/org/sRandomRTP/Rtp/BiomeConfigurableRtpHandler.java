package org.sRandomRTP.Rtp;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.clampRadiusToBorder;

public final class BiomeConfigurableRtpHandler extends AbstractRtpHandler {

    private final List<Biome> biomeTargets;

    private BiomeConfigurableRtpHandler(List<Biome> biomeTargets) {
        this.biomeTargets = biomeTargets == null ? Collections.emptyList() : new ArrayList<>(biomeTargets);
    }

    public static void rtpBiome(CommandSender sender, World targetWorld, List<Biome> biomeTargets) {
        new BiomeConfigurableRtpHandler(biomeTargets).launchRtp(sender, targetWorld);
    }

    @Override
    protected List<Biome> getBiomeTargets() {
        return biomeTargets;
    }

    @Override
    protected LaunchParams buildLaunchParams(Player player, World world, boolean loggingEnabled) {
        int centerX = worldCenterX(world);
        int centerZ = worldCenterZ(world);
        int radius = resolveRadius(world, true);
        int minRadius = resolveRadius(world, false);
        DifferentRtpMethods.ClampedRadius clamped =
                clampRadiusToBorder(world, radius, minRadius, "[sRandomRTP-Biome]", loggingEnabled);
        radius = clamped.radius;
        minRadius = clamped.minRadius;
        if (!validateRadius(minRadius, radius, player)) return null;
        return new LaunchParams(centerX, centerZ, radius, minRadius, resolveMaxAttempts(world), true);
    }

    @Override
    protected int resolveMaxAttempts(World world) {
        FileConfiguration config = getConfigFile();
        String worldPath = getPerWorldBase() + world.getName() + ".maxtries";
        if (config.contains(worldPath)) {
            return Math.max(1, config.getInt(worldPath));
        }
        return Math.max(1, config.getInt(getConfigPrefix() + ".maxtries-biome", 48));
    }

    @Override
    protected String getCoordinateGenerationMethod() {
        return getConfigFile().getString(getConfigPrefix() + ".coordinate-generation-biome", "CIRCLE");
    }

    @Override
    protected boolean getUseAbsoluteCoordinates() {
        return getConfigFile().getBoolean(getConfigPrefix() + ".use-absolute-coordinates-biome", true);
    }

    @Override
    protected String resolveSearchStage(org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext context, int attemptNumber) {
        String mode = getConfigFile().getString(getConfigPrefix() + ".search-mode", "TWO_PHASE");
        return isBiomeScanStage(mode, attemptNumber, getFastRandomAttempts()) ? "biome-scan" : "fast-random";
    }

    @Override
    protected int resolveBiomeProbeSamples(org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext context, int attemptNumber) {
        String mode = getConfigFile().getString(getConfigPrefix() + ".search-mode", "TWO_PHASE");
        return resolveProbeSamples(mode, attemptNumber, getFastRandomAttempts(),
                getConfigFile().getInt(getConfigPrefix() + ".probe-samples-per-attempt", 8));
    }

    @Override
    protected int[] generateXZ(Player player, World world, int centerX, int centerZ,
                               int radius, int minRadius, int generationIndex,
                               long sessionNonce, String method, boolean absolute,
                               boolean loggingEnabled, int attemptNumber) {
        if (!"SQUARE".equalsIgnoreCase(method)) {
            return super.generateXZ(player, world, centerX, centerZ, radius, minRadius, generationIndex,
                    sessionNonce, method, absolute, loggingEnabled, attemptNumber);
        }
        return generateSquareRingCoordinates(player, centerX, centerZ, radius, minRadius, generationIndex, sessionNonce);
    }

    private FileConfiguration getConfigFile() {
        return Variables.biomefile;
    }

    private String getConfigPrefix() {
        return "teleport-biome";
    }

    private String getPerWorldBase() {
        return getConfigPrefix() + ".per-world-biome.";
    }

    private int getFastRandomAttempts() {
        return Math.max(1, getConfigFile().getInt(getConfigPrefix() + ".fast-random-attempts", 12));
    }

    static boolean isBiomeScanStage(String searchMode, int attemptNumber, int fastRandomAttempts) {
        if (!"TWO_PHASE".equalsIgnoreCase(searchMode)) {
            return false;
        }
        return attemptNumber > Math.max(1, fastRandomAttempts);
    }

    static int resolveProbeSamples(String searchMode, int attemptNumber, int fastRandomAttempts, int configuredProbeSamples) {
        if (!isBiomeScanStage(searchMode, attemptNumber, fastRandomAttempts)) {
            return 1;
        }
        return Math.max(1, configuredProbeSamples);
    }

    private int resolveRadius(World world, boolean isRadius) {
        return resolveRadiusFromConfig(getConfigFile(), getPerWorldBase(), world.getName(),
                getConfigPrefix() + ".radius-biome", getConfigPrefix() + ".minradius-biome",
                isRadius, isRadius ? 30000 : 10000);
    }

    private int[] generateSquareRingCoordinates(Player player, int centerX, int centerZ, int radius,
                                                int minRadius, int generationIndex, long sessionNonce) {
        long baseSeed = player != null ? player.getName().hashCode() : 0L;
        long combinedSeed = mixSeeds(baseSeed, sessionNonce ^ (generationIndex * 31L + 17L));
        SplittableRandom random = generationIndex >= 0
                ? Variables.getRngProvider().deterministic(combinedSeed)
                : Variables.getRngProvider().deterministic(System.nanoTime());

        int effectiveRadius = Math.max(1, radius);
        int effectiveMin = Math.max(0, Math.min(minRadius, effectiveRadius - 1));

        for (int guard = 0; guard < 32; guard++) {
            int deltaX = nextRange(random, -effectiveRadius, effectiveRadius);
            int deltaZ = nextRange(random, -effectiveRadius, effectiveRadius);
            if (Math.abs(deltaX) < effectiveMin && Math.abs(deltaZ) < effectiveMin) {
                continue;
            }
            return new int[]{centerX + deltaX, centerZ + deltaZ};
        }

        return new int[]{centerX + effectiveRadius, centerZ};
    }

    private int nextRange(SplittableRandom random, int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        }
        return random.nextInt(minInclusive, maxInclusive + 1);
    }

    private long mixSeeds(long baseSeed, long sessionNonce) {
        long mixed = baseSeed ^ sessionNonce;
        mixed ^= (mixed >>> 30);
        mixed *= 0xbf58476d1ce4e5b9L;
        mixed ^= (mixed >>> 27);
        mixed *= 0x94d049bb133111ebL;
        mixed ^= (mixed >>> 31);
        return mixed;
    }
}
