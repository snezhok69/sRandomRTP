package org.sRandomRTP.DataPortals;

import org.sRandomRTP.DifferentMethods.Variables;
import java.util.Map;

public class PortalDataBlocks {

    private final String playerName;
    private final String world;
    private final int x;
    private final int y;
    private final int z;
    private final String blockType;
    private final String portalName;

    public PortalDataBlocks(String playerName, String world, int x, int y, int z, String blockType, String portalName) {
        this.playerName = playerName;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
        this.portalName = portalName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getWorld() {
        return world;
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

    public String getBlockType() {
        return blockType;
    }

    public String getPortalName() {
        return portalName;
    }

    public static Map<String, PortalDataBlocks> getPortalBlocksCache() {
        return Variables.getRuntimeState().getPlayerPortalsBlocks();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalDataBlocks)) return false;
        PortalDataBlocks that = (PortalDataBlocks) o;
        return x == that.x && y == that.y && z == that.z
                && java.util.Objects.equals(world, that.world)
                && java.util.Objects.equals(portalName, that.portalName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(world, portalName, x, y, z);
    }
}
