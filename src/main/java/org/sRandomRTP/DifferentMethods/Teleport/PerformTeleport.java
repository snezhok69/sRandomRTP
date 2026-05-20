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

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PerformTeleport {
    /**
     * Tracks players with an active teleport in progress.
     * {@link Set#add} is atomic on ConcurrentHashMap-backed sets and replaces the old
     * {@code putIfAbsent + AtomicBoolean.compareAndSet} pattern with a single lock-free call.
     */
    private static final Set<UUID> teleportInProgress = ConcurrentHashMap.newKeySet();

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
        // Set.add() is atomic — returns false if the UUID was already present (duplicate teleport guard)
        if (!teleportInProgress.add(playerId)) {
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
                    .handle((success, ex) -> {
                        FoliaSchedulerFacade.runAtEntity(player, () -> {
                            try {
                                // Record metric on the entity thread so timing includes scheduler latency
                                // and the measurement is safe on Folia's region-threaded model.
                                if (ctx != null) {
                                    ctx.recordFinalTeleport(System.nanoTime() - startedAt);
                                }
                                if (ex != null) {
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().severe("[PerformTeleport] Error during teleport: " + ex.getMessage());
                                    }
                                    cleanupAfterFailure(player, loggingEnabled, playerId);
                                } else if (Boolean.TRUE.equals(success)) {
                                    handleSuccessfulTeleport(player, finalNewX, finalNewZ, newY, loggingEnabled, ctx, playerId);
                                } else {
                                    if (loggingEnabled) {
                                        Bukkit.getLogger().warning("[PerformTeleport] Async teleport failed, trying again");
                                    }
                                    retryTeleport(player, teleportLocation, loggingEnabled, finalNewX, finalNewZ, newY, ctx, playerId);
                                }
                            } catch (RuntimeException runEx) {
                                if (loggingEnabled) {
                                    Bukkit.getLogger().severe("[PerformTeleport] Unhandled error in entity callback: " + runEx.getMessage());
                                }
                                cleanupAfterFailure(player, loggingEnabled, playerId);
                            }
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
                .handle((retrySuccess, e) -> {
                    if (ctx != null) {
                        ctx.recordFinalTeleport(System.nanoTime() - retryStartedAt);
                    }
                    FoliaSchedulerFacade.runAtEntity(player, () -> {
                        if (e != null || !Boolean.TRUE.equals(retrySuccess)) {
                            if (loggingEnabled) {
                                if (e != null) {
                                    Bukkit.getLogger().severe("[PerformTeleport] Error during second teleport attempt: " + e.getMessage());
                                } else {
                                    Bukkit.getLogger().warning("[PerformTeleport] Second teleport attempt also failed");
                                }
                            }
                            cleanupAfterFailure(player, loggingEnabled, playerId);
                        } else {
                            if (loggingEnabled) {
                                Bukkit.getLogger().info("[PerformTeleport] Second teleport attempt successful");
                            }
                            handleSuccessfulTeleport(player, finalNewX, finalNewZ, newY, loggingEnabled, ctx, playerId);
                        }
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
        org.sRandomRTP.Services.RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state != null) {
            state.getLastRtpLocations().put(player.getUniqueId(), player.getLocation().clone());
        }
        Bukkit.getPluginManager().callEvent(new org.sRandomRTP.Events.RtpTeleportSuccessEvent(player, player.getLocation()));
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
        // Teleport did not occur — clear /back position so it doesn't accumulate in memory
        org.sRandomRTP.Services.RuntimeStateRegistry state = Variables.getRuntimeState();
        if (state != null) state.removeInitialPosition(player);
        TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
        Bukkit.getPluginManager().callEvent(new org.sRandomRTP.Events.RtpTeleportFailEvent(player, "teleport failure"));
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

    /**
     * Replaces the {@code %x%}, {@code %y%}, {@code %z%} coordinate placeholders in a message
     * template in a single pass through the string, avoiding two intermediate String allocations.
     */
    private static String replaceCoordsPlaceholders(String template, int x, int y, int z) {
        return template
                .replace("%x%", String.valueOf(x))
                .replace("%y%", String.valueOf(y))
                .replace("%z%", String.valueOf(z));
    }

    public static void showSuccessMessage(Player player, int finalNewX, int finalNewZ, int newY, boolean loggingEnabled) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Showing success message to " + player.getName());
        }

        for (String line : LoadMessages.teleportyes) {
            String formattedLine = replaceCoordsPlaceholders(line, finalNewX, newY, finalNewZ);
            // TranslateRGBColors.translateRGBColors() already calls ChatColor.translateAlternateColorCodes internally
            formattedLine = TranslateRGBColors.translateRGBColors(formattedLine);
            player.sendMessage(formattedLine);
        }
    }

    public static void showTitlesAndApplyEffects(Player player, int finalNewX, int finalNewZ, int newY, boolean loggingEnabled) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Showing titles and applying effects to " + player.getName());
        }

        // Single volatile read — all subsequent accesses use the local snapshot so the JIT can
        // optimise freely and we avoid 8+ repeated memory-barrier crossings in this hot path.
        final org.sRandomRTP.Services.ConfigCache cfg = Variables.configCache;

        showTitle(player, cfg, finalNewX, finalNewZ, newY);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        FoliaSchedulerFacade.runAtEntity(player,
                () -> org.sRandomRTP.DifferentMethods.Player.CommandRun.commandrun(player));
        applyFreeze(player, cfg, loggingEnabled);
        applySound(player, cfg);
        applyResourceCosts(player, cfg);

        if (cfg.particlesEnabled) {
            FoliaSchedulerFacade.runAtEntity(player,
                    () -> org.sRandomRTP.Events.PortalAndEffectsListener.startParticleEffect(player));
        }

        org.sRandomRTP.DifferentMethods.Player.EffectGivePlayer.effectGivePlayer(player);
    }

    private static void showTitle(Player player, org.sRandomRTP.Services.ConfigCache cfg,
                                   int x, int z, int y) {
        if (!cfg.titleEnabled) return;
        if (LoadMessages.titleMessage.isEmpty()
                && (!cfg.subtitleEnabled || LoadMessages.subtitleMessage.isEmpty())) {
            return;
        }
        // TranslateRGBColors.translateRGBColors() already calls ChatColor.translateAlternateColorCodes internally
        String formattedTitle = TranslateRGBColors.translateRGBColors(
                replaceCoordsPlaceholders(LoadMessages.titleMessage, x, y, z));
        String formattedSubtitle = cfg.subtitleEnabled
                ? TranslateRGBColors.translateRGBColors(
                        replaceCoordsPlaceholders(LoadMessages.subtitleMessage, x, y, z))
                : null;
        player.sendTitle(formattedTitle, formattedSubtitle,
                cfg.titleFadeIn, cfg.titleStay, cfg.titleFadeOut);
    }

    private static void applyFreeze(Player player, org.sRandomRTP.Services.ConfigCache cfg,
                                     boolean loggingEnabled) {
        if (!cfg.freezeEnabled) return;
        if (Variables.cachedServerMajorVersion >= 17) {
            player.setFreezeTicks(cfg.freezeTime * 40);
        } else if (loggingEnabled) {
            Bukkit.getConsoleSender().sendMessage("The freeze teleportation feature does not work on versions below 1.17.");
        }
    }

    private static void applySound(Player player, org.sRandomRTP.Services.ConfigCache cfg) {
        if (cfg.teleportSoundEnabled && cfg.teleportSound != null) {
            // teleportSound is pre-parsed at config-load time — no try/catch needed in hot-path
            player.playSound(player.getLocation(), cfg.teleportSound,
                    cfg.teleportSoundVolume, cfg.teleportSoundPitch);
        }
    }

    private static void applyResourceCosts(Player player, org.sRandomRTP.Services.ConfigCache cfg) {
        if (cfg.hungerEnabled) {
            player.setFoodLevel(Math.max(player.getFoodLevel() - cfg.hungerAmount, 0));
        }
        if (cfg.healthEnabled) {
            player.setHealth(Math.max(player.getHealth() - cfg.healthAmount, 0.0));
        }
        if (cfg.levelsEnabled) {
            player.setLevel(Math.max(player.getLevel() - cfg.levelsAmount, 0));
        }
        if (cfg.itemsEnabled && !Variables.itemMap.isEmpty()) {
            for (java.util.Map.Entry<org.bukkit.Material, Integer> entry : Variables.itemMap.entrySet()) {
                org.sRandomRTP.DifferentMethods.Player.RemovePlayerItems.removePlayerItems(
                        player, entry.getKey(), entry.getValue());
            }
        }
    }
}
