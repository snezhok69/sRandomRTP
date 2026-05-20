package org.sRandomRTP.Events;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RtpSearchStartEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final World world;

    public RtpSearchStartEvent(Player player, World world) {
        this.player = player;
        this.world = world;
    }

    public Player getPlayer() {
        return player;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
