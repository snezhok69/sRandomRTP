package org.sRandomRTP.DifferentMethods.Teleport;

import io.papermc.lib.PaperLib;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class PerformTeleport {
    private static final java.util.Map<UUID, AtomicBoolean> teleportInProgress = new java.util.concurrent.ConcurrentHashMap<>();

    public static void performTeleport(Player player, Location teleportLocation, boolean loggingEnabled, int finalNewX, int finalNewZ, int newY) {
        if (loggingEnabled) {
            Bukkit.getLogger().info("[PerformTeleport] Starting teleport for player " + player.getName() + " to " + teleportLocation);
        }

        UUID playerId = player.getUniqueId();
        AtomicBoolean inProgress = teleportInProgress.computeIfAbsent(playerId, k -> new AtomicBoolean(false));
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
        if (ctx != null && (ctx.isCancelled() || ctx.isExpired())) {
            if (loggingEnabled) {
                Bukkit.getLogger().info("[PerformTeleport] Aborting teleport for player " + player.getName() + " due to cancel/expired");
            }
            teleportInProgress.remove(playerId);
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            return;
        }

        try {
            PaperLib.teleportAsync(player, teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                    .thenAccept(success -> {
                        if (success) {
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
                        } else {
                            if (loggingEnabled) {
                                Bukkit.getLogger().warning("[PerformTeleport] Async teleport failed, trying again");
                            }
                            PaperLib.teleportAsync(player, teleportLocation, PlayerTeleportEvent.TeleportCause.PLUGIN)
                                    .thenAccept(retrySuccess -> {
                                        if (retrySuccess) {
                                            if (loggingEnabled) {
                                                Bukkit.getLogger().info("[PerformTeleport] Second teleport attempt successful");
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
                                        } else {
                                            if (loggingEnabled) {
                                                Bukkit.getLogger().warning("[PerformTeleport] Second teleport attempt also failed");
                                            }
                                            CleanupTasks.cleanupTasks(player, loggingEnabled);
                                            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                                            TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
                                            teleportInProgress.remove(playerId);
                                        }
                                    })
                                    .exceptionally(e -> {
                                        if (loggingEnabled) {
                                            Bukkit.getLogger().severe("[PerformTeleport] Error during second teleport attempt: " + e.getMessage());
                                        }
                                        CleanupTasks.cleanupTasks(player, loggingEnabled);
                                        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                                        TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
                                        teleportInProgress.remove(playerId);
                                        return null;
                                    });
                        }
                    })
                    .exceptionally(ex -> {
                        if (loggingEnabled) {
                            Bukkit.getLogger().severe("[PerformTeleport] Error during teleport: " + ex.getMessage());
                        }
                        CleanupTasks.cleanupTasks(player, loggingEnabled);
                        CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
                        TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
                        teleportInProgress.remove(playerId);
                        return null;
                    });
        } catch (Exception e) {
            if (loggingEnabled) {
                Bukkit.getLogger().severe("[PerformTeleport] Critical error during teleport setup: " + e.getMessage());
            }
            CleanupTasks.cleanupTasks(player, loggingEnabled);
            CleanupTasks.finalizeTeleportStatus(player, loggingEnabled);
            TeleportRequestManager.cancelRequest(playerId, loggingEnabled, "teleport failure");
            teleportInProgress.remove(playerId);
        }
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

        boolean titleEnabled = Variables.titlefile.getBoolean("teleport.titleEnabled");
        boolean subtitleEnabled = Variables.titlefile.getBoolean("teleport.subtitleEnabled");

        if (titleEnabled && (!LoadMessages.titleMessage.isEmpty() || (subtitleEnabled && !LoadMessages.subtitleMessage.isEmpty()))) {
            String formattedTitle = LoadMessages.titleMessage
                    .replace("%x%", String.valueOf(finalNewX))
                    .replace("%y%", String.valueOf(newY))
                    .replace("%z%", String.valueOf(finalNewZ));

            formattedTitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedTitle));

            if (subtitleEnabled) {
                String formattedSubtitle = LoadMessages.subtitleMessage
                        .replace("%x%", String.valueOf(finalNewX))
                        .replace("%y%", String.valueOf(newY))
                        .replace("%z%", String.valueOf(finalNewZ));

                formattedSubtitle = TranslateRGBColors.translateRGBColors(ChatColor.translateAlternateColorCodes('&', formattedSubtitle));

                player.sendTitle(formattedTitle, formattedSubtitle,
                        (int) (Variables.titlefile.getDouble("teleport.titleFadeIn") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.titleStay") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.titleFadeOut") * 20));
            } else {
                player.sendTitle(formattedTitle, null,
                        (int) (Variables.titlefile.getDouble("teleport.titleFadeIn") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.titleStay") * 20),
                        (int) (Variables.titlefile.getDouble("teleport.titleFadeOut") * 20));
            }
        }

        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));

        Variables.getFoliaLib().getImpl().runAtEntity(
                player,
                (ignored) -> org.sRandomRTP.DifferentMethods.Player.CommandRun.commandrun(player)
        );

        if (Variables.effectfile.getBoolean("teleport.Freeze.enabled")) {
            String version = Bukkit.getServer().getVersion();
            int majorVersion = Integer.parseInt(version.split("\\.")[1]);

            if (majorVersion >= 17) {
                int freezeTimeInSeconds = Variables.effectfile.getInt("teleport.Freeze.time");
                int freezeTicks = freezeTimeInSeconds * 40;
                player.setFreezeTicks(freezeTicks);
            } else {
                Bukkit.getConsoleSender().sendMessage("The freeze teleportation feature does not work on versions below 1.17.");
            }
        }

        if (Variables.soundfile.getBoolean("teleport.completed-teleport-sound.enabled")) {
            String soundName = Variables.soundfile.getString("teleport.completed-teleport-sound.sound");
            float volume = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.volume");
            float pitch = (float) Variables.soundfile.getDouble("teleport.completed-teleport-sound.pitch");

            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException ex) {
                Bukkit.getConsoleSender().sendMessage("Invalid sound name in config: " + soundName);
            }
        }

        if (Variables.economyfile.getBoolean("teleport.Hunger.enabled")) {
            int newFoodLevel = player.getFoodLevel() - Variables.economyfile.getInt("teleport.Hunger.hunger");
            player.setFoodLevel(Math.max(newFoodLevel, 0));
        }

        if (Variables.economyfile.getBoolean("teleport.Health.enabled")) {
            double healthToDeduct = Variables.economyfile.getDouble("teleport.Health.health");
            double newHealth = player.getHealth() - healthToDeduct;
            player.setHealth(Math.max(newHealth, 0.0));
        }

        if (Variables.economyfile.getBoolean("teleport.Levels.enabled")) {
            int newLevel = player.getLevel() - Variables.economyfile.getInt("teleport.Levels.level");
            player.setLevel(Math.max(newLevel, 0));
        }

        if (Variables.economyfile.getBoolean("teleport.Items.enabled")) {
            for (java.util.Map.Entry<org.bukkit.Material, Integer> entry : Variables.itemMap.entrySet()) {
                org.sRandomRTP.DifferentMethods.Player.RemovePlayerItems.removePlayerItems(player, entry.getKey(), entry.getValue());
            }
        }

        if (Variables.particlesfile.getBoolean("teleport.particles.enabled")) {
            Variables.getFoliaLib().getImpl().runAtEntity(player, (e) -> org.sRandomRTP.Events.PlayerParticles.playerParticles(player));
        }

        org.sRandomRTP.DifferentMethods.Player.EffectGivePlayer.effectGivePlayer(player);
    }
}
