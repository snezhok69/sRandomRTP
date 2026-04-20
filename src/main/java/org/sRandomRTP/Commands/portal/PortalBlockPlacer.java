package org.sRandomRTP.Commands.portal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.sRandomRTP.Commands.PortalShape;
import org.sRandomRTP.DataPortals.PortalDataBlocks;
import org.sRandomRTP.DataPortals.PortalDataSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Static utility that handles all block-placement operations for portal creation.
 * Extracted from {@link org.sRandomRTP.Commands.CommandSetPortal} to give block
 * placement a single, focused home.
 *
 * <p>All methods operate on a {@link PortalCreationContext} rather than carrying
 * 8-10 individual parameters. Block data strings are returned as {@code List<String>}
 * and joined by the caller; the context itself remains fully immutable.</p>
 */
public final class PortalBlockPlacer {

    private PortalBlockPlacer() {}

    /**
     * Returns {@code true} if the area for the portal floor and border is within the
     * world border and all affected blocks are {@link Material#AIR}.
     */
    public static boolean isAreaAvailable(Location center, int radius, PortalShape shape) {
        World world = center.getWorld();
        if (world == null) return false;
        WorldBorder border = world.getWorldBorder();
        Location borderCenter = border.getCenter();
        int half = (int) border.getSize() / 2;
        int minX = borderCenter.getBlockX() - half;
        int maxX = borderCenter.getBlockX() + half;
        int minZ = borderCenter.getBlockZ() - half;
        int maxZ = borderCenter.getBlockZ() + half;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!shape.isCoordinateInFloor(x, z, radius)) continue;
                Location glassLocation = center.clone().add(x, -1, z);
                Block block = glassLocation.getBlock();
                if (glassLocation.getX() < minX || glassLocation.getX() > maxX
                        || glassLocation.getZ() < minZ || glassLocation.getZ() > maxZ) {
                    return false;
                }
                if (block.getType() != Material.AIR) {
                    return false;
                }
            }
        }
        if (shape == PortalShape.CIRCLE) {
            double increment = (2 * Math.PI) / (radius * 16);
            for (double angle = 0; angle < 2 * Math.PI; angle += increment) {
                double x = center.getX() + (radius * Math.cos(angle));
                double z = center.getZ() + (radius * Math.sin(angle));
                Location borderLocation = new Location(world, x, center.getY(), z);
                Block block = borderLocation.getBlock();
                if (borderLocation.getX() < minX || borderLocation.getX() > maxX
                        || borderLocation.getZ() < minZ || borderLocation.getZ() > maxZ) {
                    return false;
                }
                if (block.getType() != Material.AIR) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Places the glass floor tiles for the portal.
     *
     * @return serialised block data strings for each placed block
     */
    public static List<String> placeFloor(PortalCreationContext ctx, Location center, int radius, PortalShape shape) {
        Material floorMaterial = ctx.settings().getFloorMaterial();
        List<String> blockDataList = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (!shape.isCoordinateInFloor(x, z, radius)) continue;
                blockDataList.add(placePortalBlock(ctx, center.clone().add(x, -1, z), floorMaterial));
            }
        }
        return blockDataList;
    }

    /**
     * Places the obsidian (or configured material) border around the portal floor.
     *
     * @return serialised block data strings for each placed block
     */
    public static List<String> placeBorder(PortalCreationContext ctx, Location center, int radius, PortalShape shape) {
        List<String> blockDataList = new ArrayList<>();
        if (shape == PortalShape.CIRCLE) {
            int x = 0;
            int z = radius;
            int d = 3 - 2 * radius;
            while (z >= x) {
                if (!isUnwantedBlock(x, z, radius)) {
                    blockDataList.add(placeObsidianBorder(ctx, center, x, z));
                    blockDataList.add(placeObsidianBorder(ctx, center, -x, z));
                    blockDataList.add(placeObsidianBorder(ctx, center, x, -z));
                    blockDataList.add(placeObsidianBorder(ctx, center, -x, -z));
                    blockDataList.add(placeObsidianBorder(ctx, center, z, x));
                    blockDataList.add(placeObsidianBorder(ctx, center, -z, x));
                    blockDataList.add(placeObsidianBorder(ctx, center, z, -x));
                    blockDataList.add(placeObsidianBorder(ctx, center, -z, -x));
                }
                if (d < 0) {
                    d = d + 4 * x + 6;
                } else {
                    d = d + 4 * (x - z) + 10;
                    z--;
                }
                x++;
            }
        } else { // SQUARE
            for (int bx = -radius; bx <= radius; bx++) {
                blockDataList.add(placeObsidianBorder(ctx, center, bx, -radius));
                blockDataList.add(placeObsidianBorder(ctx, center, bx, radius));
            }
            for (int bz = -radius + 1; bz <= radius - 1; bz++) {
                blockDataList.add(placeObsidianBorder(ctx, center, -radius, bz));
                blockDataList.add(placeObsidianBorder(ctx, center, radius, bz));
            }
        }
        return blockDataList;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Places a single portal block, records its original type in placedBlocks,
     * registers it in the portal block spatial index, and returns its serialised form.
     *
     * @return the serialised block data string for this block
     */
    private static String placePortalBlock(PortalCreationContext ctx, Location loc, Material material) {
        Block block = loc.getBlock();
        if (!ctx.state().getPlacedBlocks().containsKey(loc)) {
            ctx.state().getPlacedBlocks().put(loc, block.getType());
        }
        block.setType(material);

        String blockData = ctx.worldName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + ","
                + loc.getBlockZ() + ":" + material.name();

        String key = ctx.player().getName() + ":" + ctx.portalName() + ":" + ctx.worldName() + ":"
                + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        ctx.state().putPortalBlock(key, new PortalDataBlocks(ctx.player().getName(), ctx.worldName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                material.name(), ctx.portalName()));

        return blockData;
    }

    private static String placeObsidianBorder(PortalCreationContext ctx, Location center, int x, int z) {
        Location borderLocation = center.clone().add(x, 0, z);
        String blockData = placePortalBlock(ctx, borderLocation, ctx.borderMaterial());

        Particle borderParticle = ctx.settings().getBorderParticle();
        int particleCount = ctx.settings().getBorderParticleCount();
        double particleSpread = ctx.settings().getBorderParticleSpread();
        if (center.getWorld() != null) {
            center.getWorld().spawnParticle(borderParticle, borderLocation.clone().add(0.5, 0.5, 0.5),
                    particleCount, particleSpread, particleSpread, particleSpread, 0);
        }
        return blockData;
    }

    private static boolean isUnwantedBlock(int x, int z, int radius) {
        if (radius % 2 == 0 && radius <= 5) {
            return Math.abs(x) == Math.abs(z);
        }
        return false;
    }
}
