package org.sRandomRTP.DataPortals;

public class PortalData {
    private String playerName;
    private String worldName;
    private String portalName;
    private double x;
    private double y;
    private double z;
    private String shape;

    public PortalData(String playerName, String worldName, String portalName, double x, double y, double z) {
        this(playerName, worldName, portalName, x, y, z, "circle");
    }

    public PortalData(String playerName, String worldName, String portalName, double x, double y, double z, String shape) {
        this.playerName = playerName;
        this.worldName = worldName;
        this.portalName = portalName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shape = shape;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getWorldName() {
        return worldName;
    }

    public String getPortalName() {
        return portalName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalData)) return false;
        PortalData that = (PortalData) o;
        return java.util.Objects.equals(playerName, that.playerName)
                && java.util.Objects.equals(portalName, that.portalName);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(playerName, portalName);
    }
}
