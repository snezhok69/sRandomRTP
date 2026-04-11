package org.sRandomRTP.Rtp;

import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods;

import org.bukkit.command.CommandSender;

import static org.sRandomRTP.DifferentMethods.rtprtps.DifferentRtpMethods.clampRadiusToBorder;

/**
 * Shared base for Far and Middle RTP handlers.
 * Subclasses provide config file, key prefix, and log prefix — all logic is shared.
 * <p>
 * Use the static factory methods {@link #rtpFar} and {@link #rtpMiddle} as entry points
 * instead of the former standalone {@code RtpRtpFar} / {@code RtpRtpMiddle} classes.
 */
public abstract class RadiusConfigurableRtpHandler extends AbstractRtpHandler {

    /** Entry point for /rtp far — replaces the former RtpRtpFar class. */
    public static void rtpFar(CommandSender sender, World targetWorld) {
        new FarHandler().launchRtp(sender, targetWorld);
    }

    /** Entry point for /rtp middle — replaces the former RtpRtpMiddle class. */
    public static void rtpMiddle(CommandSender sender, World targetWorld) {
        new MiddleHandler().launchRtp(sender, targetWorld);
    }

    private static final class FarHandler extends RadiusConfigurableRtpHandler {
        private static final String COORD_KEY     = "teleport-far.coordinate-generation-far";
        private static final String ABSOLUTE_KEY  = "teleport-far.use-absolute-coordinates-far";
        private static final String RADIUS_KEY    = "teleport-far.radius-far";
        private static final String MIN_RADIUS_KEY = "teleport-far.minradius-far";
        private static final String PER_WORLD_BASE = "teleport-far.per-world-far.";

        @Override protected FileConfiguration getConfigFile()   { return Variables.farfile; }
        @Override protected String getConfigPrefix()            { return "teleport-far"; }
        @Override protected String getRadiusKeyFull()           { return RADIUS_KEY; }
        @Override protected String getMinRadiusKeyFull()        { return MIN_RADIUS_KEY; }
        @Override protected String getPerWorldBase()            { return PER_WORLD_BASE; }
        @Override protected String getLogPrefix()               { return "[sRandomRTP-Far]"; }
        @Override protected String getCoordinateGenerationMethod() { return getConfigFile().getString(COORD_KEY); }
        @Override protected boolean getUseAbsoluteCoordinates() { return getConfigFile().getBoolean(ABSOLUTE_KEY); }
    }

    private static final class MiddleHandler extends RadiusConfigurableRtpHandler {
        private static final String COORD_KEY     = "teleport-middle.coordinate-generation-middle";
        private static final String ABSOLUTE_KEY  = "teleport-middle.use-absolute-coordinates-middle";
        private static final String RADIUS_KEY    = "teleport-middle.radius-middle";
        private static final String MIN_RADIUS_KEY = "teleport-middle.minradius-middle";
        private static final String PER_WORLD_BASE = "teleport-middle.per-world-middle.";

        @Override protected FileConfiguration getConfigFile()   { return Variables.middlefile; }
        @Override protected String getConfigPrefix()            { return "teleport-middle"; }
        @Override protected String getRadiusKeyFull()           { return RADIUS_KEY; }
        @Override protected String getMinRadiusKeyFull()        { return MIN_RADIUS_KEY; }
        @Override protected String getPerWorldBase()            { return PER_WORLD_BASE; }
        @Override protected String getLogPrefix()               { return "[sRandomRTP-Middle]"; }
        @Override protected String getCoordinateGenerationMethod() { return getConfigFile().getString(COORD_KEY); }
        @Override protected boolean getUseAbsoluteCoordinates() { return getConfigFile().getBoolean(ABSOLUTE_KEY); }
    }


    protected abstract FileConfiguration getConfigFile();

    /** e.g. "teleport-far" or "teleport-middle" */
    protected abstract String getConfigPrefix();

    /** Full config key for the radius, e.g. "teleport-far.radius-far" */
    protected abstract String getRadiusKeyFull();

    /** Full config key for the min radius, e.g. "teleport-far.minradius-far" */
    protected abstract String getMinRadiusKeyFull();

    /** Per-world config key base, e.g. "teleport-far.per-world-far." */
    protected abstract String getPerWorldBase();

    /** e.g. "[sRandomRTP-Far]" or "[sRandomRTP-Middle]" */
    protected abstract String getLogPrefix();

    private int resolveRadius(World world, boolean isRadius) {
        return resolveRadiusFromConfig(getConfigFile(), getPerWorldBase(), world.getName(),
                getRadiusKeyFull(), getMinRadiusKeyFull(), isRadius, 0);
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
        return new LaunchParams(centerX, centerZ, radius, minRadius, Variables.cachedMaxTries, true);
    }

}
