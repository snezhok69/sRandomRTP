package org.sRandomRTP.Commands.portal;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.sRandomRTP.Commands.PortalShape;
import org.sRandomRTP.DifferentMethods.Teleport.FoliaSchedulerFacade;
import org.sRandomRTP.DifferentMethods.Variables;

/**
 * Schedules portal runtime work on the portal location thread instead of async timers.
 */
public final class PortalTaskScheduler {

    private PortalTaskScheduler() {
    }

    public static WrappedTask scheduleParticles(Location center, int radius, PortalShape shape,
                                                long delayTicks, long periodTicks) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        return FoliaSchedulerFacade.runAtLocationTimer(center,
                () -> PortalParticleManager.spawnParticles(center, radius, shape),
                delayTicks, periodTicks);
    }

    public static WrappedTask scheduleParticles(Location center, int radius, String shape,
                                                long delayTicks, long periodTicks) {
        return scheduleParticles(center, radius, PortalShape.fromString(shape), delayTicks, periodTicks);
    }

    public static WrappedTask scheduleTrigger(Location center, int radius, PortalShape shape,
                                              long delayTicks, long periodTicks) {
        if (center == null || center.getWorld() == null) {
            return null;
        }
        PortalTriggerHandler triggerHandler = Variables.getPortalTriggerHandler();
        if (triggerHandler == null) {
            return null;
        }
        return FoliaSchedulerFacade.runAtLocationTimer(center,
                () -> triggerHandler.handlePortalTrigger(center, radius, shape),
                delayTicks, periodTicks);
    }

    public static WrappedTask scheduleTrigger(Location center, int radius, String shape,
                                              long delayTicks, long periodTicks) {
        return scheduleTrigger(center, radius, PortalShape.fromString(shape), delayTicks, periodTicks);
    }
}
