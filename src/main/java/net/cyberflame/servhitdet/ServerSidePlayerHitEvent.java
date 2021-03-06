package net.cyberflame.servhitdet;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServerSidePlayerHitEvent extends Event implements Cancellable {

    private boolean cancelled;
    private Player attacker;
    private Player victim;

    private static final HandlerList handlers = new HandlerList();

    public ServerSidePlayerHitEvent(Player attacker, Player victim) {
        this.attacker = attacker;
        this.victim = victim;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
