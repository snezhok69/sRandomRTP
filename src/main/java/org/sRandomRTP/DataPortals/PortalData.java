package org.sRandomRTP.DataPortals;

public class PortalData {
    private String playerName;
    private String worldName;
    private String portalName;
    private String x;
    private String y;
    private String z;
    private String shape;

    public PortalData(String playerName, String worldName, String portalName, String x, String y, String z) {
        this(playerName, worldName, portalName, x, y, z, "circle"); // По умолчанию круглый
    }

    public PortalData(String playerName, String worldName, String portalName, String x, String y, String z, String shape) {
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

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getZ() {
        return z;
    }

    public String getShape() {
        return shape;
    }

    public void setShape(String shape) {
        this.shape = shape;
    }
}