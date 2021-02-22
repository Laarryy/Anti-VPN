package me.egg82.antivpn.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Plugin;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.util.UUID;

public class LoginEventWrapper {
    private PreLoginEvent preLoginEvent = null;
    private PostLoginEvent postLoginEvent = null;
    private LoginEvent loginEvent = null;
    private Plugin plugin;

    public LoginEventWrapper(Object event,Plugin plugin) {
        if (event instanceof PreLoginEvent) {
            preLoginEvent = (PreLoginEvent) event;
        }else if (event instanceof PostLoginEvent) {
            postLoginEvent = (PostLoginEvent) event;
        } else {
            loginEvent = (LoginEvent) event;
        }
        this.plugin = plugin;
    }

    public InetSocketAddress getAddress() {
        if (preLoginEvent != null) {
            return preLoginEvent.getConnection().getAddress();
        } else if (postLoginEvent != null) {
            return postLoginEvent.getPlayer().getAddress();
        } else {
            return loginEvent.getConnection().getAddress();
        }
    }
    public void disconnect(String ip,boolean mcLeaks,BaseComponent... message){
        if (preLoginEvent != null) {
            preLoginEvent.setCancelled(true);
            preLoginEvent.setCancelReason(message);
        } else if (postLoginEvent != null) {
            postLoginEvent.getPlayer().disconnect(message);
        } else {
            loginEvent.setCancelled(true);
            loginEvent.setCancelReason(message);
        }
        plugin.getLogger().info("Disconnecting " + getConnection().getName() + " for using a" + (mcLeaks ? "n MCLeaks account" : " VPN") + ". Address: " + ip);
    }

    public UUID getUniqueId() {
        if (preLoginEvent != null) {
            throw new IllegalArgumentException("PreLoginEvent doesn't have method getUniqueID");
        } else if (postLoginEvent != null) {
            return postLoginEvent.getPlayer().getUniqueId();
        } else {
            return loginEvent.getConnection().getUniqueId();
        }
    }

    public String getName() {
        if (preLoginEvent != null) {
            return preLoginEvent.getConnection().getName();
        } else if (postLoginEvent != null) {
            return postLoginEvent.getPlayer().getName();
        } else {
            return loginEvent.getConnection().getName();
        }
    }

    @Nullable
    public ProxiedPlayer getPlayer() {
        if (preLoginEvent != null) {
            return null;
        } else if (postLoginEvent != null) {
            return postLoginEvent.getPlayer();
        } else {
            return null;
        }
    }

    public void setCancelReason(BaseComponent... cancelReason) {
        if (preLoginEvent != null) {
            preLoginEvent.setCancelReason(cancelReason);
        } else if (postLoginEvent != null) {
           throw new IllegalArgumentException("PostLoginEvent doesn't have method setCancelReason");
        } else {
            loginEvent.setCancelReason(cancelReason);
        }
    }

    public PendingConnection getConnection() {
        if (preLoginEvent != null) {
            return preLoginEvent.getConnection();
        } else if (postLoginEvent != null) {
            throw new IllegalArgumentException("PostLoginEvent doesn't have method getConnection");
        } else {
            return loginEvent.getConnection();
        }
    }

    public void setCancelled(boolean cancelled) {
        if (preLoginEvent != null) {
            preLoginEvent.setCancelled(true);
        } else if (postLoginEvent != null) {
            throw new IllegalArgumentException("PostLoginEvent doesn't have method setCancelReason");
        } else {
            loginEvent.setCancelled(true);
        }
    }

}
