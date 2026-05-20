package org.sRandomRTP.Commands.portal;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.PortalShape;
import org.sRandomRTP.Services.PortalSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility responsible for spawning portal particle effects.
 * Extracted from {@link org.sRandomRTP.Commands.CommandSetPortal} to give
 * particle rendering a single, focused home.
 */
public final class PortalParticleManager {
    private static final int MAX_CACHE_SIZE = 512;
    private static final double VISIBILITY_PADDING = 24.0D;
    private static final Map<String, List<RelativePoint>> FLOOR_POINT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, List<RelativePoint>> BORDER_POINT_CACHE = new ConcurrentHashMap<>();

    private PortalParticleManager() {}

    /**
     * String overload retained for {@link org.sRandomRTP.DataPortals.PortalSQLRepository}
     * which passes the stored shape string from the database on load.
     */
    public static void spawnParticles(Location center, int radius, String shapeStr) {
        spawnParticles(center, radius, PortalShape.fromString(shapeStr));
    }

    /** Spawns floor and (optionally) border particles for the portal. */
    public static void spawnParticles(Location center, int radius, PortalShape shape) {
        if (center == null || center.getWorld() == null) return;
        if (!hasNearbyPlayer(center, radius + VISIBILITY_PADDING)) return;
        PortalSettings settings = PortalSettings.current();
        Particle floorParticle = settings.getFloorParticle();
        int floorParticleCount = settings.getFloorParticleCount();
        double floorParticleYOffset = -0.5;
        double floorDensity = settings.getFloorParticleDensity();
        double floorSpread = settings.getFloorParticleSpread();

        for (RelativePoint point : floorPoints(radius, shape, floorDensity, floorParticleYOffset)) {
            center.getWorld().spawnParticle(floorParticle, point.toLocation(center),
                    floorParticleCount, floorSpread, floorSpread, floorSpread, 0);
        }

        if (settings.isPermanentBorderParticlesEnabled()) {
            spawnBorderParticles(center, radius, shape);
        }
    }

    private static void spawnBorderParticles(Location center, int radius, PortalShape shape) {
        PortalSettings settings = PortalSettings.current();
        Particle borderParticle = settings.getBorderParticle();
        int borderParticleCount = settings.getBorderParticleCount();
        double borderParticleYOffset = settings.isPermanentBorderParticlesEnabled() ? 0.3 : -0.7;
        double borderDensity = settings.getBorderParticleDensity();
        double borderSpread = settings.getBorderParticleSpread();

        for (RelativePoint point : borderPoints(radius, shape, borderDensity, borderParticleYOffset)) {
            center.getWorld().spawnParticle(borderParticle, point.toLocation(center),
                    borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
        }
    }

    private static List<RelativePoint> floorPoints(int radius, PortalShape shape, double density, double yOffset) {
        String key = "floor:" + shape + ':' + radius + ':' + density + ':' + yOffset;
        List<RelativePoint> cached = FLOOR_POINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        List<RelativePoint> points = new ArrayList<>();
        for (double x = -radius; x <= radius; x += density) {
            for (double z = -radius; z <= radius; z += density) {
                if (shape.isCoordinateInFloor((int) x, (int) z, radius)) {
                    points.add(new RelativePoint(x, yOffset, z));
                }
            }
        }
        cache(FLOOR_POINT_CACHE, key, points);
        return points;
    }

    private static List<RelativePoint> borderPoints(int radius, PortalShape shape, double density, double yOffset) {
        String key = "border:" + shape + ':' + radius + ':' + density + ':' + yOffset;
        List<RelativePoint> cached = BORDER_POINT_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        List<RelativePoint> points = new ArrayList<>();
        if (shape == PortalShape.CIRCLE) {
            double increment = (2 * Math.PI) / Math.max(1.0D, radius * (1 / density) * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                points.add(new RelativePoint(radius * Math.cos(angle), yOffset, radius * Math.sin(angle)));
            }
        } else {
            for (double x = -radius; x <= radius; x += density) {
                points.add(new RelativePoint(x, yOffset, -radius));
                points.add(new RelativePoint(x, yOffset, radius));
            }
            for (double z = -radius + density; z < radius; z += density) {
                points.add(new RelativePoint(-radius, yOffset, z));
                points.add(new RelativePoint(radius, yOffset, z));
            }
        }
        cache(BORDER_POINT_CACHE, key, points);
        return points;
    }

    private static void cache(Map<String, List<RelativePoint>> cache, String key, List<RelativePoint> points) {
        if (cache.size() >= MAX_CACHE_SIZE) {
            cache.clear();
        }
        cache.put(key, java.util.Collections.unmodifiableList(points));
    }

    private static boolean hasNearbyPlayer(Location center, double range) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (entity instanceof Player && ((Player) entity).isOnline()) {
                return true;
            }
        }
        return false;
    }

    private static final class RelativePoint {
        private final double x;
        private final double y;
        private final double z;

        private RelativePoint(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        private Location toLocation(Location center) {
            return center.clone().add(x, y, z);
        }
    }
}
