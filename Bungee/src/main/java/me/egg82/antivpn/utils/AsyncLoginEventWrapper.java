package me.egg82.antivpn.utils;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;

public class AsyncLoginEventWrapper {
    PreLoginEvent preLoginEvent = null;
    LoginEvent loginEvent = null;

    public AsyncLoginEventWrapper(Object event) {
        if (event instanceof PreLoginEvent) {
            preLoginEvent = (PreLoginEvent) event;
        } else {
            loginEvent = (LoginEvent) event;
        }
    }

    public void setCancelReason(BaseComponent... cancelReason) {
        if (preLoginEvent != null) {
            preLoginEvent.setCancelReason(cancelReason);
        } else {
            loginEvent.setCancelReason(cancelReason);
        }
    }

    public PendingConnection getConnection() {
        if (preLoginEvent != null) {
            return preLoginEvent.getConnection();
        } else {
            return loginEvent.getConnection();
        }
    }

    public void setCancelled(boolean cancelled) {
        if (preLoginEvent != null) {
            preLoginEvent.setCancelled(true);
        } else {
            loginEvent.setCancelled(true);
        }
    }

}
