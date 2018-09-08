package me.egg82.avpn.core;

import ninja.egg82.patterns.events.EventArgs;

public class UpdateEventArgs extends EventArgs {
    // vars
    public static UpdateEventArgs EMPTY = new UpdateEventArgs(null, false, -1L);

    private String ip = null;
    private boolean value = false;
    private long created = -1L;

    // constructor
    public UpdateEventArgs(String ip, boolean value, long created) {
        this.ip = ip;
        this.value = value;
        this.created = created;
    }

    // public
    public String getIp() {
        return ip;
    }

    public boolean getValue() {
        return value;
    }

    public long getCreated() {
        return created;
    }

    // private

}
