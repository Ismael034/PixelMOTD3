package dev.mruniverse.pixelmotd.bungeecord.listeners.whitelist.type;

import dev.mruniverse.pixelmotd.bungeecord.PixelMOTDBuilder;
import dev.mruniverse.pixelmotd.bungeecord.listeners.whitelist.AbstractWhitelistListener;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class ListenerHigh extends AbstractWhitelistListener implements Listener {
    public ListenerHigh(PixelMOTDBuilder plugin) {

        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(LoginEvent event) {
        checkPlayer(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnect(ServerConnectEvent event) {
        checkPlayer(event);
    }
}
