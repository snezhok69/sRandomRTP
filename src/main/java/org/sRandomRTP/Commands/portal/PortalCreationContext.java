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
 * and joined by the caller, keeping this record fully immutable.</p>
 */
public record PortalCreationContext(
        Player player,
        String portalName,
        String worldName,
        Material borderMaterial,
        PortalSettings settings,
        RuntimeStateRegistry state
) {}
