package org.sRandomRTP.DataPortals;

import java.util.Objects;

/**
 * Common base for portal data objects whose identity is determined by (playerName, portalName).
 */
abstract class PortalReference {

    protected final String playerName;
    protected final String portalName;

    protected PortalReference(String playerName, String portalName) {
        this.playerName = playerName;
        this.portalName = portalName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getPortalName() {
        return portalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortalReference)) return false;
        PortalReference that = (PortalReference) o;
        return Objects.equals(playerName, that.playerName)
                && Objects.equals(portalName, that.portalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, portalName);
    }
}
