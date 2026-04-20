package org.sRandomRTP.DifferentMethods.Teleport;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.sRandomRTP.DifferentMethods.Variables;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class FoliaSchedulerFacade {

    private static final Logger LOG = Logger.getLogger(FoliaSchedulerFacade.class.getName());
    /**
     * Emitted once when we fall back to inline execution because FoliaLib is not yet initialised.
     * Using a once-only flag avoids log spam on every call while still making the condition visible.
     */
    private static final AtomicBoolean INLINE_FALLBACK_WARNED = new AtomicBoolean(false);

    private FoliaSchedulerFacade() {
    }

    public static void runAtLocation(Location location, Runnable runnable) {
        if (location == null || runnable == null) return;
        requireFoliaOrPrimary("runAtLocation");
        if (Variables.getFoliaLib() != null) {
            Variables.getFoliaLib().getImpl().runAtLocation(location, ignored -> runnable.run());
            return;
        }
        warnInlineFallback("runAtLocation");
        runnable.run();
    }

    public static void runAtEntity(Entity entity, Runnable runnable) {
        if (entity == null || runnable == null) return;
        requireFoliaOrPrimary("runAtEntity");
        if (Variables.getFoliaLib() != null) {
            Variables.getFoliaLib().getImpl().runAtEntity(entity, ignored -> runnable.run());
            return;
        }
        warnInlineFallback("runAtEntity");
        runnable.run();
    }

    public static WrappedTask runLater(Runnable runnable, long delayTicks) {
        if (runnable == null) return null;
        if (Variables.getFoliaLib() == null) {
            if (delayTicks <= 0L && Bukkit.isPrimaryThread()) {
                runnable.run();
                return null;
            }
            throw new IllegalStateException("FoliaLib is not initialized and delayed execution cannot be scheduled safely");
        }
        return Variables.getFoliaLib().getImpl().runLater(runnable, Math.max(0L, delayTicks));
    }

    public static WrappedTask runLaterEntityAware(Player player, long delayTicks, Runnable runnable) {
        if (player == null || runnable == null) return null;
        return runLater(() -> runAtEntity(player, runnable), delayTicks);
    }

    /**
     * Throws {@link IllegalStateException} if FoliaLib is not available and we are not on the
     * primary thread — the only two scheduler paths that are safe for synchronous dispatch.
     */
    private static void requireFoliaOrPrimary(String methodName) {
        if (Variables.getFoliaLib() == null && !Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(
                    "FoliaSchedulerFacade." + methodName + ": FoliaLib is not initialized and no safe scheduler fallback is available");
        }
    }

    /**
     * Logs a one-time warning when we fall back to inline execution because FoliaLib is null.
     * Suppressed after the first emission so post-initialisation hot-path code is never spammed.
     */
    private static void warnInlineFallback(String methodName) {
        if (INLINE_FALLBACK_WARNED.compareAndSet(false, true)) {
            LOG.warning("[sRandomRTP] FoliaSchedulerFacade." + methodName
                    + ": FoliaLib is not yet initialised — running inline on primary thread."
                    + " This is expected during plugin startup but should not occur during gameplay.");
        }
    }
}
