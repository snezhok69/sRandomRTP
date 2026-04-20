package org.sRandomRTP.DifferentMethods.Teleport;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.sRandomRTP.DifferentMethods.LoggerUtility;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CompatibleTeleport {

    private static final AtomicBoolean FALLBACK_WARNING_REPORTED = new AtomicBoolean(false);

    private CompatibleTeleport() {
    }

    public static CompletableFuture<Boolean> teleport(Player player,
                                                      Location location,
                                                      PlayerTeleportEvent.TeleportCause cause,
                                                      boolean loggingEnabled,
                                                      String context) {
        if (player == null || location == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        try {
            CompletableFuture<Boolean> asyncTeleport = PaperLib.teleportAsync(player, location, cause);
            if (asyncTeleport == null) {
                return fallbackTeleport(player, location, loggingEnabled, context, null);
            }

            CompletableFuture<Boolean> result = new CompletableFuture<>();
            asyncTeleport.whenComplete((success, throwable) -> {
                if (throwable == null) {
                    result.complete(Boolean.TRUE.equals(success));
                    return;
                }

                if (isCompatibilityFailure(throwable)) {
                    fallbackTeleport(player, location, loggingEnabled, context, throwable)
                            .whenComplete((fallbackSuccess, fallbackThrowable) -> {
                                if (fallbackThrowable != null) {
                                    result.completeExceptionally(fallbackThrowable);
                                } else {
                                    result.complete(Boolean.TRUE.equals(fallbackSuccess));
                                }
                            });
                    return;
                }

                if (loggingEnabled) {
                    Bukkit.getLogger().warning("[sRandomRTP] Teleport failed for " + context + ": " + throwable.getMessage());
                }
                LoggerUtility.loggerUtility(CompatibleTeleport.class, unwrap(throwable));
                result.completeExceptionally(throwable);
            });
            return result;
        } catch (LinkageError | RuntimeException throwable) {
            if (isCompatibilityFailure(throwable)) {
                return fallbackTeleport(player, location, loggingEnabled, context, throwable);
            }
            if (loggingEnabled) {
                Bukkit.getLogger().warning("[sRandomRTP] Teleport setup failed for " + context + ": " + throwable.getMessage());
            }
            LoggerUtility.loggerUtility(CompatibleTeleport.class, unwrap(throwable));
            CompletableFuture<Boolean> failed = new CompletableFuture<>();
            failed.completeExceptionally(throwable);
            return failed;
        }
    }

    public static CompletableFuture<Boolean> teleportLoadedChunk(Player player,
                                                                 Location location,
                                                                 boolean loggingEnabled,
                                                                 String context) {
        if (player == null || location == null) {
            return CompletableFuture.completedFuture(Boolean.FALSE);
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        FoliaSchedulerFacade.runAtEntity(player, () -> {
            try {
                result.complete(player.teleport(location));
            } catch (LinkageError | RuntimeException throwable) {
                if (loggingEnabled) {
                    Bukkit.getLogger().warning("[sRandomRTP] Sync teleport failed for " + context + ": "
                            + throwable.getMessage());
                }
                LoggerUtility.loggerUtility(CompatibleTeleport.class, unwrap(throwable));
                result.completeExceptionally(throwable);
            }
        });
        return result;
    }

    private static CompletableFuture<Boolean> fallbackTeleport(Player player,
                                                               Location location,
                                                               boolean loggingEnabled,
                                                               String context,
                                                               Throwable sourceFailure) {
        if (FALLBACK_WARNING_REPORTED.compareAndSet(false, true)) {
            Bukkit.getLogger().warning("[sRandomRTP] Async Paper teleport API is not fully compatible on this server. "
                    + "Using synchronous fallback teleport where needed.");
        }
        if (loggingEnabled && sourceFailure != null) {
            Bukkit.getLogger().warning("[sRandomRTP] Falling back to sync teleport for " + context + ": "
                    + sourceFailure.getClass().getSimpleName() + " - " + sourceFailure.getMessage());
        }

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        teleportLoadedChunk(player, location, loggingEnabled, context + " fallback")
                .whenComplete((success, throwable) -> {
                    if (throwable != null) {
                        if (loggingEnabled) {
                            Bukkit.getLogger().warning("[sRandomRTP] Sync fallback teleport failed for " + context + ": "
                                    + throwable.getMessage());
                        }
                        result.completeExceptionally(throwable);
                    } else {
                        result.complete(Boolean.TRUE.equals(success));
                    }
                });
        return result;
    }

    private static boolean isCompatibilityFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof NoSuchMethodError || current instanceof AbstractMethodError) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                || current instanceof java.util.concurrent.ExecutionException)) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }
}
