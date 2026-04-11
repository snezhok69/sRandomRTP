package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

public final class FoliaSchedulerFacade {

    private FoliaSchedulerFacade() {
    }

    public static void runAtLocation(Location location, Runnable runnable) {
        if (location == null || runnable == null) {
            return;
        }

        if (Variables.getFoliaLib() != null) {
            Variables.getFoliaLib().getImpl().runAtLocation(location, ignored -> runnable.run());
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        throw new IllegalStateException("FoliaLib is not initialized and no safe scheduler fallback is available");
    }

    public static void runAtEntity(Entity entity, Runnable runnable) {
        if (entity == null || runnable == null) {
            return;
        }

        if (Variables.getFoliaLib() != null) {
            Variables.getFoliaLib().getImpl().runAtEntity(entity, ignored -> runnable.run());
            return;
        }

        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }

        throw new IllegalStateException("FoliaLib is not initialized and no safe scheduler fallback is available");
    }

    public static WrappedTask runLater(Runnable runnable, long delayTicks) {
        if (runnable == null) {
            return null;
        }
        if (Variables.getFoliaLib() == null) {
            if (delayTicks <= 0L && Bukkit.isPrimaryThread()) {
                runnable.run();
                return null;
            }
            throw new IllegalStateException("FoliaLib is not initialized and delayed execution cannot be scheduled safely");
        }
        return Variables.getFoliaLib().getImpl().runLater(runnable, Math.max(0L, delayTicks));
    }

    public static WrappedTask runLaterEntityAware(final Player player, long delayTicks, final Runnable runnable) {
        if (player == null || runnable == null) {
            return null;
        }
        return runLater(new Runnable() {
            @Override
            public void run() {
                runAtEntity(player, runnable);
            }
        }, delayTicks);
    }
}
