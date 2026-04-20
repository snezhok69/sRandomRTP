package org.sRandomRTP.Commands.portal;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.sRandomRTP.Commands.PortalShape;
import org.sRandomRTP.Services.PortalSettings;

/**
 * Static utility responsible for spawning portal particle effects.
 * Extracted from {@link org.sRandomRTP.Commands.CommandSetPortal} to give
 * particle rendering a single, focused home.
 */
public final class PortalParticleManager {

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
        PortalSettings settings = PortalSettings.current();
        Particle floorParticle = settings.getFloorParticle();
        int floorParticleCount = settings.getFloorParticleCount();
        double floorParticleYOffset = -0.5;
        double floorDensity = settings.getFloorParticleDensity();
        double floorSpread = settings.getFloorParticleSpread();

        for (double x = -radius; x <= radius; x += floorDensity) {
            for (double z = -radius; z <= radius; z += floorDensity) {
                if (!shape.isCoordinateInFloor((int) x, (int) z, radius)) continue;
                Location particleLocation = center.clone().add(x, floorParticleYOffset, z);
                center.getWorld().spawnParticle(floorParticle, particleLocation,
                        floorParticleCount, floorSpread, floorSpread, floorSpread, 0);
            }
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

        if (shape == PortalShape.CIRCLE) {
            double increment = (2 * Math.PI) / (radius * (1 / borderDensity) * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = radius * Math.cos(angle);
                double z = radius * Math.sin(angle);
                Location particleLocation = center.clone().add(x, borderParticleYOffset, z);
                center.getWorld().spawnParticle(borderParticle, particleLocation,
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        } else { // SQUARE
            for (double x = -radius; x <= radius; x += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, -radius),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(x, borderParticleYOffset, radius),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
            for (double z = -radius + borderDensity; z < radius; z += borderDensity) {
                center.getWorld().spawnParticle(borderParticle, center.clone().add(-radius, borderParticleYOffset, z),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
                center.getWorld().spawnParticle(borderParticle, center.clone().add(radius, borderParticleYOffset, z),
                        borderParticleCount, borderSpread, borderSpread, borderSpread, 0);
            }
        }
    }
}
