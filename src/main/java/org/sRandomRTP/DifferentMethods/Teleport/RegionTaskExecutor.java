package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.sRandomRTP.DifferentMethods.Variables;

/**
 * Utility wrapper around Folia's region scheduler that gracefully falls back to the
 * Bukkit main thread when Folia is not available. This allows the teleport pipeline
 * to execute world and chunk interactions on the correct thread, regardless of
 * whether the server is running Folia or a traditional single-threaded implementation.
 */
public final class RegionTaskExecutor {

    private RegionTaskExecutor() {
    }

    /**
     * Ensures that the supplied runnable is executed on the region thread that owns the
     * provided location. On Folia we delegate to FoliaLib's region scheduler, otherwise the
     * runnable is executed synchronously on the Bukkit main thread.
     *
     * @param location the world location that determines the owning region
     * @param runnable the work to execute
     */
    public static void runAtLocation(Location location, Runnable runnable) {
        if (location == null || runnable == null) {
            return;
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            Variables.getFoliaLib().getImpl().runAtLocation(location, ignored -> runnable.run());
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(Variables.getInstance(), runnable);
        }
    }

    /**
     * Executes a runnable on the scheduler that owns the provided entity. This method mirrors
     * {@link #runAtLocation(Location, Runnable)} but uses the entity handle instead of a fixed
     * location.
     *
     * @param entity   the entity whose region should own the work
     * @param runnable the work to execute
     */
    public static void runAtEntity(Entity entity, Runnable runnable) {
        if (entity == null || runnable == null) {
            return;
        }

        if (Variables.getFoliaLib() != null && Variables.getFoliaLib().isFolia()) {
            Variables.getFoliaLib().getImpl().runAtEntity(entity, ignored -> runnable.run());
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(Variables.getInstance(), runnable);
        }
    }

    /**
     * Convenience helper to copy a location without mutating the original reference.
     * FoliaLib expects immutable locations when scheduling tasks, so we clone the provided
     * location to ensure thread safety.
     *
     * @param location original location
     * @return cloned location or {@code null} when the original was {@code null}
     */
    public static Location cloneLocation(Location location) {
        return location == null ? null : location.clone();
    }
}
