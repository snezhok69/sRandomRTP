package org.sRandomRTP.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
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

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        try {
            if (!Variables.cachedMoveCancelRtp) return;
            if (event.getTo() == null) return;

            Player player = event.getPlayer();
            if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                    && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
                // No block movement — check for mouse rotation if enabled
                if (Variables.cachedMouseMoveCancelRtp
                        && (event.getFrom().getYaw() != event.getTo().getYaw()
                            || event.getFrom().getPitch() != event.getTo().getPitch())) {
                    TeleportCancellationSupport.cancelIfActive(
                            player,
                            "mouse-move cancel",
                            LoadMessages.teleportmovecancel,
                            !Variables.cachedMouseMoveCancelCooldown
                    );
                }
                return;
            }

            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "move cancel",
                    LoadMessages.teleportmovecancel,
                    !Variables.cachedMoveCancelCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent event) {
        handleBlockEvent(event.getPlayer());
    }

    private void handleBlockEvent(Player player) {
        try {
            if (!Variables.cachedBreakBlockCancelRtp) return;
            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "break-block cancel",
                    LoadMessages.teleportmovecancel,
                    !Variables.cachedBreakBlockCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        try {
            if (!Variables.cachedDamagedCancelRtp) return;
            if (!(event.getEntity() instanceof Player)) return;

            Player player = (Player) event.getEntity();
            TeleportCancellationSupport.cancelIfActive(
                    player,
                    "damage cancel",
                    LoadMessages.teleportdamagedcancel,
                    !Variables.cachedDmgCancelCooldown
            );
        } catch (RuntimeException e) {
            LoggerUtility.loggerUtility(TeleportCancellationListener.class, e);
        }
    }
}
