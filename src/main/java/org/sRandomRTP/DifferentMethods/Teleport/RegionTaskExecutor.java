package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

public final class RegionTaskExecutor {

    private RegionTaskExecutor() {
    }

    public static void runAtLocation(Location location, Runnable runnable) {
        FoliaSchedulerFacade.runAtLocation(location, runnable);
    }

    public static void runAtEntity(Entity entity, Runnable runnable) {
        FoliaSchedulerFacade.runAtEntity(entity, runnable);
    }

    public static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }
}
