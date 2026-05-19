package org.sRandomRTP.Commands.portal;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.sRandomRTP.Services.PortalSettings;
import org.sRandomRTP.Services.RuntimeStateRegistry;

/**
 * Immutable context object passed through the portal creation pipeline.
 * Replaces the 8-10 individual parameters previously threaded through
 * {@code placePortalBlock}, {@code placeObsidianBorder}, and helpers.
 *
 * <p>Block data strings (previously accumulated in a mutable {@code StringBuilder})
 * are now returned by {@link PortalBlockPlacer} methods as {@code List<String>}
 * and joined by the caller, keeping this context fully immutable.</p>
 */
public final class PortalCreationContext {
    private final Player player;
    private final String portalName;
    private final String worldName;
    private final Material borderMaterial;
    private final PortalSettings settings;
    private final RuntimeStateRegistry state;

    public PortalCreationContext(Player player,
                                 String portalName,
                                 String worldName,
                                 Material borderMaterial,
                                 PortalSettings settings,
                                 RuntimeStateRegistry state) {
        this.player = player;
        this.portalName = portalName;
        this.worldName = worldName;
        this.borderMaterial = borderMaterial;
        this.settings = settings;
        this.state = state;
    }

    public Player player() {
        return player;
    }

    public String portalName() {
        return portalName;
    }

    public String worldName() {
        return worldName;
    }

    public Material borderMaterial() {
        return borderMaterial;
    }

    public PortalSettings settings() {
        return settings;
    }

    public RuntimeStateRegistry state() {
        return state;
    }
}
