package org.sRandomRTP.DifferentMethods.Teleport;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.sRandomRTP.DifferentMethods.EconomyPaymentManager;
import org.sRandomRTP.DifferentMethods.Text.TranslateRGBColors;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestManager;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportRequestContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformTeleport {
    private static final java.util.Map<UUID, AtomicBoolean> teleportInProgress = new java.util.concurrent.ConcurrentHashMap<>();

    public static void performTeleport(Player player, RtpCandidateResolution resolution, boolean loggingEnabled) {
        if (resolution == null) {
            performTeleport(player, null, loggingEnabled, 0, 0, 0);
            return;
        }
        performTeleport(player, resolution, resolution.toLocation(), loggingEnabled, resolution.getX(), resolution.getZ(), resolution.getY());
    }

    public static void performTeleport(Player player, Location teleportLocation, boolean loggingEnabled, int finalNewX, int finalNewZ, int newY) {
        performTeleport(player, null, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY);
    }

    private static void performTeleport(Player player, RtpCandidateResolution resolution, Location teleportLocation,
                                        boolean loggingEnabled, int finalNewX, int finalNewZ, int newY) {
        if (player == null || teleportLocation == null) {
            return;
        }
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Starting teleport for player " + player.getName() + " to " + teleportLocation);
        }

        UUID playerId = player.getUniqueId();
        AtomicBoolean guard = new AtomicBoolean(false);
        AtomicBoolean existing = teleportInProgress.putIfAbsent(playerId, guard);
        AtomicBoolean inProgress = (existing != null) ? existing : guard;
        if (!inProgress.compareAndSet(false, true)) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[PerformTeleport] Teleportation already in progress for player " + player.getName() + ", skipping duplicate teleport");
            }
            return;
        }

        if (!player.isOnline()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[PerformTeleport] Player " + player.getName() + " is offline, teleportation canceled");
            }
            teleportInProgress.remove(playerId);
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            return;
        }

        final TeleportRequestContext ctx = TeleportRequestManager.getContext(playerId);
        if (ctx != null && ctx.isInactive()) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[PerformTeleport] Aborting teleport for player " + player.getName() + " due to cancel/expired");
            }
            teleportInProgress.remove(playerId);
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            return;
        }

        try {
            final long startedAt = System.nanoTime();
            selectTeleportPath(player, resolution, teleportLocation, loggingEnabled)
                    .thenAccept(success -> {
                        FoliaSchedulerFacade.runAtEntity(player, () -> {
                            // Record metric on the entity thread so timing includes scheduler latency
                            // and the measurement is safe on Folia's region-threaded model.
                            if (ctx != null) {
                                ctx.recordFinalTeleport(System.nanoTime() - startedAt);
                            }
                            if (success) {
                                handleSuccessfulTeleport(player, finalNewX, finalNewZ, newY, loggingEnabled, ctx, playerId);
                            } else {
                                if (loggingEnabled) {
                                    Bukkit.getLogger().warning("[PerformTeleport] Async teleport failed, trying again");
                                }
                                retryTeleport(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY, ctx, playerId);
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        FoliaSchedulerFacade.runAtEntity(player, () -> {
                            if (ctx != null) {
                                ctx.recordFinalTeleport(System.nanoTime() - startedAt);
                            }
                            if (loggingEnabled) {
                                Bukkit.getLogger().severe("[PerformTeleport] Error during teleport: " + ex.getMessage());
                            }
                            cleanupAfterFailure(player, loggingEnabled, playerId);
                        });
                        return null;
                    });
        } catch (RuntimeException e) {
            if (loggingEnabled) {
                Bukkit.getLogger().severe("[PerformTeleport] Critical error during teleport setup: " + e.getMessage());
            }
            cleanupAfterFailure(player, loggingEnabled, playerId);
        }
    }

    private static void retryTeleport(Player player, Location teleportLocation, boolean loggingEnabled,
                                      int finalNewX, int finalNewZ, int newY,
                                      TeleportRequestContext ctx, UUID playerId) {
        final long retryStartedAt = System.nanoTime();
        CompatibleTeleport.teleport(player, teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN,
                        loggingEnabled, "rtp teleport retry")
                .thenAccept(retrySuccess -> {
                    if (ctx != null) {
                        ctx.recordFinalTeleport(System.nanoTime() - retryStartedAt);
                    }
                    FoliaSchedulerFacade.runAtEntity(player, () -> {
                        if (retrySuccess) {
                            if (loggingEnabled) {
                                Bukkit.getLogger().info("[PerformTeleport] Second teleport attempt successful");
                            }
                            handleSuccessfulTeleport(player, finalNewX, finalNewZ, newY, loggingEnabled, ctx, playerId);
                        } else {
                            if (loggingEnabled) {
                                Bukkit.getLogger().warning("[PerformTeleport] Second teleport attempt also failed");
                            }
                            cleanupAfterFailure(player, loggingEnabled, playerId);
                        }
                    });
                })
                .exceptionally(e -> {
                    if (ctx != null) {
                        ctx.recordFinalTeleport(System.nanoTime() - retryStartedAt);
                    }
                    FoliaSchedulerFacade.runAtEntity(player, () -> {
                        if (loggingEnabled) {
                            Bukkit.getLogger().severe("[PerformTeleport] Error during second teleport attempt: " + e.getMessage());
                        }
                        cleanupAfterFailure(player, loggingEnabled, playerId);
                    });
                    return null;
                });
    }

    static CompletableFuture<Boolean> selectTeleportPath(Player player,
                                                         RtpCandidateResolution resolution,
                                                         Location teleportLocation,
                                                         boolean loggingEnabled) {
        if (shouldPreferSynchronousTeleport(resolution)) {
            return CompatibleTeleport.teleportLoadedChunk(player, teleportLocation, loggingEnabled, "rtp loaded-chunk teleport");
        }
        return CompatibleTeleport.teleport(player, teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN,
                loggingEnabled, "rtp teleport");
    }

    static boolean shouldPreferSynchronousTeleport(RtpCandidateResolution resolution) {
        return resolution != null && resolution.isChunkReadyForSynchronousTeleport();
    }

    private static void handleSuccessfulTeleport(Player player, int finalNewX, int finalNewZ, int newY,
                                                 boolean loggingEnabled, TeleportRequestContext ctx,
                                                 UUID playerId) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Player " + player.getName() + " successfully teleported");
        }
        showSuccessMessage(player, finalNewX, finalNewZ, newY, loggingEnabled);
        showTitlesAndApplyEffects(player, finalNewX, finalNewZ, newY, loggingEnabled);
        EconomyPaymentManager.confirmSuccess(player);
        CleanupTasks.cleanupTasks(player, loggingEnabled);
        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
        if (ctx != null) {
            TeleportRequestManager.completeRequest(ctx, loggingEnabled);
        }
        teleportInProgress.remove(playerId);
    }

    private static void cleanupAfterFailure(Player player, boolean loggingEnabled, UUID playerId) {
        CleanupTasks.cleanupTasks(player, loggingEnabled);
        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
        // Телепорт не состоялся — позиция /back не нужна, очищаем чтобы не копилась в памяти
        Variables.getRuntimeState().removeInitialPosition(player);
        TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
        teleportInProgress.remove(playerId);
    }

    /**
     * Removes the in-progress guard for the given player.
     * Must be called from {@code PortalAndEffectsListener.onPlayerQuit()} so that
     * a player who disconnects mid-teleport doesn't permanently block future teleports.
     */
    public static void clearInProgressForPlayer(UUID playerId) {
        teleportInProgress.remove(playerId);
    }

    /** Clears all in-progress guards. Called from {@code Main.onDisable()}. */
    public static void clearAll() {
        teleportInProgress.clear();
    }

    public static void showSuccessMessage(Player player, int finalNewX, int finalNewZ, int newY, boolean loggingEnabled) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Showing success message to " + player.getName());
        }

        List<String> formattedMessage = LoadMessages.teleportyes;
        for (String line : formattedMessage) {
            String formattedLine = line.replace("%x%", String.valueOf(finalNewX))
                    .replace("%y%", String.valueOf(newY))
                    .replace("%z%", String.valueOf(finalNewZ));

            formattedLine = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedLine));
            player.sendMessage(formattedLine);
        }
    }

    public static void showTitlesAndApplyEffects(Player player, int finalNewX, int finalNewZ, int newY, boolean loggingEnabled) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Showing titles and applying effects to " + player.getName());
        }

        if (Variables.cachedTitleEnabled
                && (!LoadMessages.titleMessage.isEmpty()
                    || (Variables.cachedSubtitleEnabled && !LoadMessages.subtitleMessage.isEmpty()))) {
            String formattedTitle = LoadMessages.titleMessage
                    .replace("%x%", String.valueOf(finalNewX))
                    .replace("%y%", String.valueOf(newY))
                    .replace("%z%", String.valueOf(finalNewZ));
            formattedTitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedTitle));

            if (Variables.cachedSubtitleEnabled) {
                String formattedSubtitle = LoadMessages.subtitleMessage
                        .replace("%x%", String.valueOf(finalNewX))
                        .replace("%y%", String.valueOf(newY))
                        .replace("%z%", String.valueOf(finalNewZ));
                formattedSubtitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedSubtitle));
                player.sendTitle(formattedTitle, formattedSubtitle,
                        Variables.cachedTitleFadeIn, Variables.cachedTitleStay, Variables.cachedTitleFadeOut);
            } else {
                player.sendTitle(formattedTitle, null,
                        Variables.cachedTitleFadeIn, Variables.cachedTitleStay, Variables.cachedTitleFadeOut);
            }
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

        Variables.getFoliaLib().getImpl().runAtEntity(
                player,
                (ignored) -> org.sRandomRTP.DifferentMethods.Player.CommandRun.commandrun(player)
        );

        if (Variables.cachedFreezeEnabled) {
            if (Variables.cachedServerMajorVersion >= 17) {
                player.setFreezeTicks(Variables.cachedFreezeTime * 40);
            } else if (loggingEnabled) {
                Bukkit.getConsoleSender().sendMessage("The freeze teleportation feature does not work on versions below 1.17.");
            }
        }

        if (Variables.cachedTeleportSoundEnabled && !Variables.cachedTeleportSoundName.isEmpty()) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(Variables.cachedTeleportSoundName.toUpperCase());
                player.playSound(player.getLocation(), sound, Variables.cachedTeleportSoundVolume, Variables.cachedTeleportSoundPitch);
            } catch (IllegalArgumentException ex) {
                Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + Variables.cachedTeleportSoundName);
            }
        }

        if (Variables.cachedHungerEnabled) {
            player.setFoodLevel(Math.max(player.getFoodLevel() - Variables.cachedHungerAmount, 0));
        }

        if (Variables.cachedHealthEnabled) {
            player.setHealth(Math.max(player.getHealth() - Variables.cachedHealthAmount, 0.0));
        }

        if (Variables.cachedLevelsEnabled) {
            player.setLevel(Math.max(player.getLevel() - Variables.cachedLevelsAmount, 0));
        }

        if (Variables.cachedItemsEnabled) {
            for (java.util.Map.Entry<org.bukkit.Material, Integer> entry : Variables.itemMap.entrySet()) {
                org.sRandomRTP.DifferentMethods.Player.RemovePlayerItems.removePlayerItems(player, entry.getKey(), entry.getValue());
            }
        }

        if (Variables.cachedParticlesEnabled) {
            Variables.getFoliaLib().getImpl().runAtEntity(player, (e) -> org.sRandomRTP.Events.PortalAndEffectsListener.startParticleEffect(player));
        }

        org.sRandomRTP.DifferentMethods.Player.EffectGivePlayer.effectGivePlayer(player);
    }
}
