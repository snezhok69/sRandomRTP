package org.sRandomRTP.GetYGet;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;
import org.sRandomRTP.DifferentMethods.Variables;

/**
 * Safe-Y search strategies for Nether and End environments.
 *
 * <p>Extracted from the inline lambdas in
 * {@link org.sRandomRTP.Rtp.AbstractRtpHandler#resolveCandidateOnLoadedChunk}.
 */
public final class NetherEndSafeYResolver {

    private NetherEndSafeYResolver() {}

    /**
     * Finds a safe Y coordinate in the Nether.
     * Rejects positions at bedrock ceiling/floor.
     *
     * @return Y coordinate, or -1 if none found
     */
    public static int netherSafeY(World world, int x, int z, TeleportRequestContext context) {
        return VerticalSafeYSearchSupport.getSafeYCoordinateOnLoadedChunk(
                NetherEndSafeYResolver.class, "[Nether]", world, x, z, context,
                Variables.configCache.minYNether,
                (w, cx, cy, cz, minY) -> {
                    if (cy < minY) return false;
                    Block feet  = w.getBlockAt(cx, cy, cz);
                    Block head  = feet.getRelative(org.bukkit.block.BlockFace.UP);
                    if (!GetSafeYCoordinate.isSafeTeleportOccupantMaterial(feet.getType())
                            || !GetSafeYCoordinate.isSafeTeleportOccupantMaterial(head.getType())) return false;
                    Block below     = feet.getRelative(org.bukkit.block.BlockFace.DOWN);
                    Block twoBelow  = below.getRelative(org.bukkit.block.BlockFace.DOWN);
                    Material belowType    = below.getType();
                    Material twoBelowType = twoBelow.getType();
                    return GetSafeYCoordinate.isSafeTeleportSupportMaterial(belowType)
                            && !GetSafeYCoordinate.BEDROCK_MATERIALS.contains(belowType)
                            && GetSafeYCoordinate.isSafeTeleportSupportMaterial(twoBelowType)
                            && !GetSafeYCoordinate.BEDROCK_MATERIALS.contains(twoBelowType);
                });
    }

    /**
     * Finds a safe Y coordinate in The End.
     *
     * @return Y coordinate, or -1 if none found
     */
    public static int endSafeY(World world, int x, int z, TeleportRequestContext context) {
        return VerticalSafeYSearchSupport.getSafeYCoordinateOnLoadedChunk(
                NetherEndSafeYResolver.class, "[End]", world, x, z, context,
                Variables.configCache.minYEnd,
                (w, cx, cy, cz, minY) -> {
                    if (cy < minY) return false;
                    Block feet  = w.getBlockAt(cx, cy, cz);
                    Block head  = feet.getRelative(org.bukkit.block.BlockFace.UP);
                    if (!GetSafeYCoordinate.isSafeTeleportOccupantMaterial(feet.getType())
                            || !GetSafeYCoordinate.isSafeTeleportOccupantMaterial(head.getType())) return false;
                    Block below     = feet.getRelative(org.bukkit.block.BlockFace.DOWN);
                    Block twoBelow  = below.getRelative(org.bukkit.block.BlockFace.DOWN);
                    return GetSafeYCoordinate.isSafeTeleportSupportMaterial(below.getType())
                            && GetSafeYCoordinate.isSafeTeleportSupportMaterial(twoBelow.getType());
                });
    }
}
