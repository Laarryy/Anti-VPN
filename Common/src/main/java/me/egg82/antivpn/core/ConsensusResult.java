package me.egg82.antivpn.core;

public class ConsensusResult {
    private final String ip;
    private final double value;
    private final long created;

    public ConsensusResult(String ip, double value, long created) {
        this.ip = ip;
        this.value = value;
        this.created = created;
    }

    public String getIp() { return ip; }

    public double getValue() { return value; }

    public long getCreated() { return created; }
}
