package org.sRandomRTP.Commands;

import org.bukkit.Location;

public enum PortalShape {
    CIRCLE {
        @Override
        public boolean isInside(Location playerLocation, Location center, int radius) {
            double dx = playerLocation.getX() - center.getX();
            double dz = playerLocation.getZ() - center.getZ();
            double dy = playerLocation.getY() - center.getY();
            boolean isOnFloor = dy >= -1 && dy <= 0;
            return isOnFloor && (dx * dx + dz * dz <= (double) radius * radius);
        }

        @Override
        public boolean isCoordinateInFloor(int dx, int dz, int radius) {
            return dx * dx + dz * dz <= radius * radius;
        }
    },
    SQUARE {
        @Override
        public boolean isInside(Location playerLocation, Location center, int radius) {
            double dx = playerLocation.getX() - center.getX();
            double dz = playerLocation.getZ() - center.getZ();
            double dy = playerLocation.getY() - center.getY();
            boolean isOnFloor = dy >= -1 && dy <= 0;
            return isOnFloor && (Math.abs(dx) <= radius && Math.abs(dz) <= radius);
        }

        @Override
        public boolean isCoordinateInFloor(int dx, int dz, int radius) {
            return true; // all (dx, dz) in [-radius, radius] are inside the square
        }
    };

    /** Returns true if the player location is inside this portal shape. */
    public abstract boolean isInside(Location playerLocation, Location center, int radius);

    /**
     * Returns true if the offset (dx, dz) relative to the portal center
     * belongs to the floor area of this shape.
     */
    public abstract boolean isCoordinateInFloor(int dx, int dz, int radius);

    /**
     * Parses a shape string ("circle" or "square") case-insensitively.
     * Returns {@link #CIRCLE} as default for unrecognised values.
     */
    public static PortalShape fromString(String value) {
        if (value != null && value.equalsIgnoreCase("square")) {
            return SQUARE;
        }
        return CIRCLE;
    }

    /** Returns the lowercase config/database string representation. */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
