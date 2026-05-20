package org.sRandomRTP.Commands;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.portal.PortalBlockPlacer;
import org.sRandomRTP.Commands.portal.PortalCreationContext;
import org.sRandomRTP.Commands.portal.PortalTaskScheduler;
import org.sRandomRTP.DataPortals.PortalData;
import org.sRandomRTP.DataPortals.PortalDataSerializer;
import org.sRandomRTP.DataPortals.PortalDataTasks;
import org.sRandomRTP.DataPortals.PortalSQLRepository;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Services.PortalSettings;
import org.sRandomRTP.Services.RuntimeStateRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handles the /portal set command — validates arguments, delegates block placement to
 * {@link PortalBlockPlacer}, schedules particle/trigger tasks via
 * {@link PortalParticleManager} and {@link PortalTriggerHandler}, then registers the
 * portal in both the in-memory state and the database.
 *
 * <p>All stateful portal behaviour (trigger detection, player cooldowns, particle
 * rendering, block placement geometry) lives in focused classes under
 * {@code org.sRandomRTP.Commands.portal}.</p>
 */
public class CommandSetPortal {

    private static final Pattern PORTAL_NAME_PATTERN = Pattern.compile("^[a-zA-Zа-яА-ЯіїєІЇЄ0-9]+$");

    public static void commandSetPortal(CommandSender sender, int radius, String portalName, String shapeStr) {
        if (!CommandUtils.requirePlayer(sender).isPresent()) return;
        if (!CommandUtils.checkPermission(sender, Permissions.PORTAL)) return;
        if (radius < 1 || radius > 10) {
            Variables.getMessageService().send(sender, LoadMessages.error_radius_portal);
            return;
        }
        if (!PORTAL_NAME_PATTERN.matcher(portalName).matches()) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name);
            return;
        }
        if (portalName.length() > 90) {
            int excessCharacters = portalName.length() - 90;
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name,
                    "%excessCharacters%", String.valueOf(excessCharacters));
            return;
        }
        PortalShape shape = PortalShape.fromString(shapeStr);
        Player player = (Player) sender;
        Location center = player.getLocation().clone();
        center.setX(Math.floor(center.getX()) + 0.5);
        center.setZ(Math.floor(center.getZ()) + 0.5);
        center.setY(center.getY() - 1);

        if (!PortalBlockPlacer.isAreaAvailable(center, radius, shape)) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_shape_radius,
                    "%shape%", shape.toString(), "%radius%", String.valueOf(radius));
            return;
        }

        // Register portal data and place blocks
        World world = center.getWorld();
        if (world == null) return;
        String worldName = world.getName();
        RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state.hasPlayerPortal(sender.getName(), portalName) || hasPortalNameCollision(state, worldName, portalName)) {
            Variables.getMessageService().send(sender, LoadMessages.error_portal_name_already_exists,
                    "%portalName%", portalName);
            return;
        }
        PortalData portalData = new PortalData(player.getName(), worldName, portalName,
                center.getX(), center.getY(), center.getZ(), shape.toString());
        state.putPlayerPortal(player.getName(), portalName, portalData);
        PortalSQLRepository.savePortalPlayerToDatabaseSQL(player.getName(), worldName, portalName,
                center.getX(), center.getY(), center.getZ(), shape.toString());

        PortalSettings settings = PortalSettings.current();
        PortalCreationContext ctx = new PortalCreationContext(
                player, portalName, worldName,
                settings.getBorderMaterial(), settings, state);
        List<String> allBlockData = new ArrayList<>();
        allBlockData.addAll(PortalBlockPlacer.placeFloor(ctx, center, radius, shape));
        allBlockData.addAll(PortalBlockPlacer.placeBorder(ctx, center, radius, shape));
        String allBlocksDataStr = String.join(PortalDataSerializer.DATA_DELIMITER, allBlockData);
        PortalSQLRepository.savePortalBlocksPlayerToDatabaseSQL(player.getName(), portalName,
                radius, allBlocksDataStr, shape.toString());

        // Schedule particle and trigger tasks
        WrappedTask particlesTask = PortalTaskScheduler.scheduleParticles(center, radius, shape, 0L, 10L);
        WrappedTask triggerTask = PortalTaskScheduler.scheduleTrigger(center, radius, shape, 0L, 20L);

        // Task IDs are deterministic (derived from portalName) — stored for record-keeping
        // but tasks are looked up at runtime exclusively by portalName, not by these IDs.
        String taskIds = portalName + "_particles <|||> " + portalName + "_trigger";
        PortalSQLRepository.savePortalTasksToDatabaseSQL(player.getName(), portalName,
                "trigger | particles", 0L, 20L, center, radius, taskIds, world, shape.toString());
        state.putPortalTask(portalName, new PortalDataTasks(player.getName(), portalName,
                "trigger | particles", 0L, 20L, center, radius, taskIds, particlesTask, triggerTask, shape.toString()));

        Variables.getMessageService().send(sender, LoadMessages.success_portal_created,
                "%portalName%", portalName, "%radius%", String.valueOf(radius), "%shape%", shape.toString());
    }

    private static boolean hasPortalNameCollision(RuntimeStateRegistry state, String worldName, String portalName) {
        if (state == null || worldName == null || portalName == null) {
            return false;
        }
        for (java.util.Map<String, PortalData> portals : state.getPlayerPortals().values()) {
            if (portals == null) {
                continue;
            }
            PortalData existing = portals.get(portalName);
            if (existing != null && worldName.equalsIgnoreCase(existing.getWorldName())) {
                return true;
            }
        }
        return false;
    }
}
