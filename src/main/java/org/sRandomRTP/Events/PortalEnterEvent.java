package org.sRandomRTP.Events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PortalEnterEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Location center;
    private final int radius;

    public PortalEnterEvent(Player player, Location center, int radius) {
        this.player = player;
        this.center = center == null ? null : center.clone();
        this.radius = radius;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getCenter() {
        return center == null ? null : center.clone();
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
