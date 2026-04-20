package org.sRandomRTP.Utils;

import org.bukkit.World;
import org.bukkit.WorldBorder;

/** Null-safe world utilities shared across RTP handlers. */
public final class WorldUtils {

    private WorldUtils() {}

    /**
     * Returns the world border center X, or 0 if the border/center is null
     * (e.g. on some modded servers).
     */
    public static int worldCenterX(World world) {
        WorldBorder b = world.getWorldBorder();
        return b != null && b.getCenter() != null ? (int) b.getCenter().getX() : 0;
    }

    /**
     * Returns the world border center Z, or 0 if the border/center is null.
     */
    public static int worldCenterZ(World world) {
        WorldBorder b = world.getWorldBorder();
        return b != null && b.getCenter() != null ? (int) b.getCenter().getZ() : 0;
    }
}
