package me.egg82.antivpn.core;

import java.util.Objects;

public class IPResult {
    private final long longIPID;
    private final String ip;

    private final int hc;

    public IPResult(long longIPID, String ip) {
        this.longIPID = longIPID;
        this.ip = ip;

        hc = Objects.hash(longIPID);
    }

    public long getLongIPID() { return longIPID; }

    public String getIP() { return ip; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPResult)) return false;
        IPResult that = (IPResult) o;
        return longIPID == that.longIPID;
    }

    public int hashCode() { return hc; }
}
