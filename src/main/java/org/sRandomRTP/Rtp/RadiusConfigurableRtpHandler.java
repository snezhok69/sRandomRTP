package org.sRandomRTP.Rtp;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;

import org.bukkit.command.CommandSender;

import java.util.function.Supplier;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.clampRadiusToBorder;

/**
 * Shared handler for Far and Middle RTP — all data flows through {@link HandlerConfig}.
 * Replaces two near-identical inner classes with one parameterized implementation.
 */
public final class RadiusConfigurableRtpHandler extends AbstractRtpHandler {

    /**
     * Configuration for a specific mode (far / middle).
     * All fields are immutable — safe to pass between threads.
     */
    private static final class HandlerConfig {
        private final String coordKey;
        private final String absoluteKey;
        private final String radiusKey;
        private final String minRadiusKey;
        private final String perWorldBase;
        private final String configPrefix;
        private final String logPrefix;
        private final Supplier<FileConfiguration> configSupplier;

        private HandlerConfig(String coordKey,
                              String absoluteKey,
                              String radiusKey,
                              String minRadiusKey,
                              String perWorldBase,
                              String configPrefix,
                              String logPrefix,
                              Supplier<FileConfiguration> configSupplier) {
            this.coordKey = coordKey;
            this.absoluteKey = absoluteKey;
            this.radiusKey = radiusKey;
            this.minRadiusKey = minRadiusKey;
            this.perWorldBase = perWorldBase;
            this.configPrefix = configPrefix;
            this.logPrefix = logPrefix;
            this.configSupplier = configSupplier;
        }
    }

    private static final HandlerConfig FAR_CONFIG = new HandlerConfig(
            "teleport-far.coordinate-generation-far",
            "teleport-far.use-absolute-coordinates-far",
            "teleport-far.radius-far",
            "teleport-far.minradius-far",
            "teleport-far.per-world-far.",
            "teleport-far",
            "[sRandomRTP-Far]",
            () -> Variables.getPluginContext().getConfigRegistry().getFarFile()
    );

    private static final HandlerConfig MIDDLE_CONFIG = new HandlerConfig(
            "teleport-middle.coordinate-generation-middle",
            "teleport-middle.use-absolute-coordinates-middle",
            "teleport-middle.radius-middle",
            "teleport-middle.minradius-middle",
            "teleport-middle.per-world-middle.",
            "teleport-middle",
            "[sRandomRTP-Middle]",
            () -> Variables.getPluginContext().getConfigRegistry().getMiddleFile()
    );

    /** Entry point for /rtp far */
    public static void rtpFar(CommandSender sender, World targetWorld) {
        new RadiusConfigurableRtpHandler(FAR_CONFIG).launchRtp(sender, targetWorld);
    }

    /** Entry point for /rtp middle */
    public static void rtpMiddle(CommandSender sender, World targetWorld) {
        new RadiusConfigurableRtpHandler(MIDDLE_CONFIG).launchRtp(sender, targetWorld);
    }

    private final HandlerConfig cfg;

    private RadiusConfigurableRtpHandler(HandlerConfig cfg) {
        this.cfg = cfg;
    }

    private FileConfiguration getConfigFile()          { return cfg.configSupplier.get(); }
    protected String getConfigPrefix()                 { return cfg.configPrefix; }
    protected String getRadiusKeyFull()                { return cfg.radiusKey; }
    protected String getMinRadiusKeyFull()             { return cfg.minRadiusKey; }
    protected String getPerWorldBase()                 { return cfg.perWorldBase; }
    protected String getLogPrefix()                    { return cfg.logPrefix; }
    protected String getCoordinateGenerationMethod()   { return getConfigFile().getString(cfg.coordKey); }
    protected boolean getUseAbsoluteCoordinates()      { return getConfigFile().getBoolean(cfg.absoluteKey); }

    private int resolveRadius(World world, boolean isRadius) {
        return resolveRadiusFromConfig(getConfigFile(), getPerWorldBase(), world.getName(),
                getRadiusKeyFull(), getMinRadiusKeyFull(), isRadius, isRadius ? 30000 : 10000);
    }

    @Override
    protected LaunchParams buildLaunchParams(Player player, World world, boolean loggingEnabled) {
        int centerX = worldCenterX(world);
        int centerZ = worldCenterZ(world);
        int radius    = resolveRadius(world, true);
        int minRadius = resolveRadius(world, false);
        DifferentRtpMethods.ClampedRadius clamped =
                clampRadiusToBorder(world, radius, minRadius, getLogPrefix(), loggingEnabled);
        radius    = clamped.radius;
        minRadius = clamped.minRadius;
        if (!validateRadius(minRadius, radius, player)) return null;
        return new LaunchParams(centerX, centerZ, radius, minRadius, Variables.configCache.maxTries, true);
    }

}
