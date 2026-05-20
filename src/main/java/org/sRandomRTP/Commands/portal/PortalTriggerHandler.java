package org.sRandomRTP.Commands.portal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.sRandomRTP.Commands.PortalShape;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;
import org.sRandomRTP.Rtp.RtpRtpSimple;
import org.sRandomRTP.Services.PortalSettings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Handles portal trigger detection: detects when players enter a portal zone,
 * applies cooldowns, and dispatches teleport or command actions.
 *
 * <p>Extracted from {@link org.sRandomRTP.Commands.CommandSetPortal} to give
 * trigger logic a single, focused home with properly encapsulated instance state.</p>
 *
 * <p>This class is registered as a singleton in {@link org.sRandomRTP.Services.PluginContext}
 * and accessed via {@link Variables#getPortalTriggerHandler()}.</p>
 */
public final class PortalTriggerHandler {

    /** Pre-compiled regex for player name sanitization — avoids per-trigger compilation. */
    private static final Pattern PLAYER_NAME_SANITIZE = Pattern.compile("[^a-zA-Z0-9_]");

    /**
     * Players currently inside each portal trigger zone.
     * Keyed by world/location/radius/shape so one portal cannot suppress another portal's entry.
     */
    private final Map<String, Set<UUID>> insidePlayersByPortal = new ConcurrentHashMap<>();
    private final Object insideLock = new Object();

    private final PortalTeleportCooldownManager cooldownManager;

    public PortalTriggerHandler(PortalTeleportCooldownManager cooldownManager) {
        this.cooldownManager = cooldownManager;
    }

    /**
     * String overload retained for {@link org.sRandomRTP.DataPortals.PortalSQLRepository}
     * which passes the stored shape string from the database on load.
     */
    public void handlePortalTrigger(Location center, int radius, String shapeStr) {
        handlePortalTrigger(center, radius, PortalShape.fromString(shapeStr));
    }

    /** Checks all players in the world and triggers portal entry for new arrivals. */
    public void handlePortalTrigger(Location center, int radius, PortalShape shape) {
        if (center == null || center.getWorld() == null) return;
        PortalSettings settings = PortalSettings.current();

        String portalKey = portalKey(center, radius, shape);

        // Snapshot the previous occupant set under the lock (brief — just a copy).
        // This avoids holding the lock across the full player iteration and the
        // triggerPortalEntry dispatch, which can schedule async tasks and send messages.
        Set<UUID> previousPlayers;
        synchronized (insideLock) {
            Set<UUID> previous = insidePlayersByPortal.get(portalKey);
            previousPlayers = previous == null ? new HashSet<UUID>() : new HashSet<>(previous);
        }

        // Determine which players are inside and which are new entrants — outside the lock.
        Set<UUID> currentPlayers = new HashSet<>();
        List<Player> newEntrants = new ArrayList<>();

        double range = Math.max(1.0D, radius + 1.0D);
        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            if (!player.isOnline()) continue;
            if (shape.isInside(player.getLocation(), center, radius)) {
                currentPlayers.add(player.getUniqueId());
                if (!previousPlayers.contains(player.getUniqueId())) {
                    newEntrants.add(player);
                }
            }
        }

        // Update the occupant set under the lock (brief — only set operations).
        synchronized (insideLock) {
            if (currentPlayers.isEmpty()) {
                insidePlayersByPortal.remove(portalKey);
            } else {
                insidePlayersByPortal.put(portalKey, currentPlayers);
            }
        }

        // Dispatch entry actions entirely outside the lock.
        for (Player player : newEntrants) {
            if (!player.isOnline()) continue; // re-check: player may have disconnected during iteration
            if (settings.isCooldownEnabled() && cooldownManager.isOnCooldown(player.getUniqueId())) {
                Variables.getMessageService().send(player, LoadMessages.error_cooldown_wait);
                continue;
            }
            Bukkit.getPluginManager().callEvent(new org.sRandomRTP.Events.PortalEnterEvent(player, center, radius));
            triggerPortalEntry(player, settings);
            if (settings.isCooldownEnabled()) {
                cooldownManager.setCooldown(player.getUniqueId(),
                        settings.getCooldownSeconds() * 1000L);
            }
        }
    }

    /**
     * Cleans up all portal-related state for a player who has left the server.
     * Prevents unbounded growth of {@code insidePlayers} and the cooldown map.
     */
    public void handlePlayerQuit(UUID playerId) {
        synchronized (insideLock) {
            for (Set<UUID> players : insidePlayersByPortal.values()) {
                players.remove(playerId);
            }
            insidePlayersByPortal.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        }
        cooldownManager.clearCooldown(playerId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void triggerPortalEntry(Player player, PortalSettings settings) {
        if (settings.isPortalRtpEnabled()) {
            teleportThroughPortalRtp(player, settings.getPortalRtpWorld());
        }
        if (settings.isPortalCommandsEnabled()) {
            executePortalCommands(player, settings.getPortalCommands());
        }
    }

    private static void teleportThroughPortalRtp(Player player, String targetWorldName) {
        if (targetWorldName == null || targetWorldName.trim().isEmpty()) return;
        org.bukkit.World targetWorld = Bukkit.getWorld(targetWorldName);
        if (targetWorld == null) {
            Bukkit.getLogger().warning("[sRandomRTP] Portal RTP world not found: " + targetWorldName);
            return;
        }
        try {
            RtpRtpSimple.launch(player, targetWorld, false, "[sRandomRTP-Portal]");
        } catch (RuntimeException e) {
            Bukkit.getLogger().warning("[sRandomRTP] Error teleporting player via portal RTP: " + e.getMessage());
        }
    }

    private static void executePortalCommands(Player player, List<String> commands) {
        if (commands == null) return;
        for (String command : commands) {
            if (command == null || command.isEmpty()) continue;
            final String safeName = PLAYER_NAME_SANITIZE.matcher(player.getName()).replaceAll("");
            final String finalCommand = command.replace("%player%", safeName);
            Variables.getFoliaLib().getImpl().runAtEntity(player, e ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand));
        }
    }

    private static String portalKey(Location center, int radius, PortalShape shape) {
        return center.getWorld().getName()
                + ':' + center.getBlockX()
                + ':' + center.getBlockY()
                + ':' + center.getBlockZ()
                + ':' + radius
                + ':' + shape;
    }
}
