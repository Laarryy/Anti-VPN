package me.egg82.antivpn.core;

public class DataResult {
    private final String ip;
    private final boolean value;
    private final long created;

    public DataResult(String ip, boolean value, long created) {
        this.ip = ip;
        this.value = value;
        this.created = created;
    }

    public String getIp() { return ip; }

    public boolean getValue() { return value; }

    public long getCreated() { return created; }
}
