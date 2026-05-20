package org.sRandomRTP.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RtpTeleportFailEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final String reason;

    public RtpTeleportFailEvent(Player player, String reason) {
        this.player = player;
        this.reason = reason == null ? "unknown" : reason;
    }

    public Player getPlayer() {
        return player;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
