package org.sRandomRTP.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.sRandomRTP.Services.ConfigCache;
import org.sRandomRTP.DifferentMethods.LoggerUtility;
import org.sRandomRTP.DifferentMethods.Teleport.TeleportCancellationSupport;
import org.sRandomRTP.DifferentMethods.Variables;
import org.sRandomRTP.Files.LoadMessages;

/**
 * Consolidated listener that cancels in-progress RTP teleports when the player
 * moves, rotates, breaks a block, or receives damage.
 * Replaces the former PlayerMove, PlayerMouseMove, PlayerBreak, PlayerDamage classes.
 */
public class TeleportCancellationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        try {
            final ConfigCache cache = Variables.configCache; // single volatile read
            if (!cache.moveCancelRtp && !cache.mouseMoveCancelRtp) return;
            if (event.getTo() == null) return;

            Player player = event.getPlayer();
            // Skip all map lookups for players not in an active search
            if (!Variables.getRuntimeState().isPlayerSearching(player)) return;
            boolean blockMoved = event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()
                    || event.getFrom().getBlockZ() != event.getTo().getBlockZ();
            if (!blockMoved) {
                // No block movement — check for mouse rotation if enabled
                if (cache.mouseMoveCancelRtp
                        && (event.getFrom().getYaw() != event.getTo().getYaw()
                            || event.getFrom().getPitch() != event.getTo().getPitch())) {
                    TeleportCancellationSupport.cancelIfActive(
                            player,
                            "mouse-move cancel",
                            LoadMessages.teleportmovecancel,
                            !cache.mouseMoveCancelCooldown
                    );
                }
                return;
            }

            if (!cache.moveCancelRtp) return;
            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "move cancel",
                    LoadMessages.teleportmovecancel,
                    !cache.moveCancelCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    private void handleBlockEvent(Player player) {
        try {
            if (!Variables.configCache.breakBlockCancelRtp) return;
            if (!Variables.getRuntimeState().isPlayerSearching(player)) return;
            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "break-block cancel",
                    LoadMessages.teleportmovecancel,
                    !Variables.configCache.breakBlockCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        try {
            if (!Variables.configCache.damagedCancelRtp) return;
            if (!(event.getEntity() instanceof Player)) return;

            Player player = (Player) event.getEntity();
            if (!Variables.getRuntimeState().isPlayerSearching(player)) return;
            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "damage cancel",
                    LoadMessages.teleportdamagedcancel,
                    !Variables.configCache.dmgCancelCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }
}
