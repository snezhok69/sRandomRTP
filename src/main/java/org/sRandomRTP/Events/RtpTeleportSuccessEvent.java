package org.sRandomRTP.Events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RtpTeleportSuccessEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location location;

    public RtpTeleportSuccessEvent(Player player, Location location) {
        this.player = player;
        this.location = location == null ? null : location.clone();
    }

    public Player getPlayer() {
        return player;
    }

    public Location getLocation() {
        return location == null ? null : location.clone();
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
