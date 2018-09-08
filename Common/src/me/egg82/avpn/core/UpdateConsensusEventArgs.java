package me.egg82.avpn.core;

import ninja.egg82.patterns.events.EventArgs;

public class UpdateConsensusEventArgs extends EventArgs {
    // vars
    public static UpdateConsensusEventArgs EMPTY = new UpdateConsensusEventArgs(null, 0.0d, -1L);

    private String ip = null;
    private double value = 0.0d;
    private long created = -1L;

    // constructor
    public UpdateConsensusEventArgs(String ip, double value, long created) {
        this.ip = ip;
        this.value = value;
        this.created = created;
    }

    // public
    public String getIp() {
        return ip;
    }

    public double getValue() {
        return value;
    }

    public long getCreated() {
        return created;
    }

    // private

}
