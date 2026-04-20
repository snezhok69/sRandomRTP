package org.sRandomRTP.DataPortals;

public class PortalData extends PortalReference {
    private String worldName;
    private double x;
    private double y;
    private double z;
    private String shape;

    public PortalData(String playerName, String worldName, String portalName, double x, double y, double z) {
        this(playerName, worldName, portalName, x, y, z, "circle");
    }

    public PortalData(String playerName, String worldName, String portalName, double x, double y, double z, String shape) {
        super(playerName, portalName);
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.shape = shape;
    }

    public String getWorldName() {
        return worldName;
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
}
