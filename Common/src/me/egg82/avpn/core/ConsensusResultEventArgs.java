package me.egg82.avpn.core;

import ninja.egg82.patterns.events.EventArgs;

public class ConsensusResultEventArgs extends EventArgs {
    // vars
    public static ConsensusResultEventArgs EMPTY = new ConsensusResultEventArgs(null, null, -1L);

    private String ip = null;
    private Double value = null;
    private long created = -1L;

    // constructor
    public ConsensusResultEventArgs(String ip, Double value, long created) {
        this.ip = ip;
        this.value = value;
        this.created = created;
    }

    // public
    public String getIp() {
        return ip;
    }

    public Double getValue() {
        return value;
    }

    public long getCreated() {
        return created;
    }

    // private

}
