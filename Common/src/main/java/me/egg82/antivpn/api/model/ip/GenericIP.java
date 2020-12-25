package me.egg82.antivpn.api.model.ip;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericIP implements IP {
    private final String ip;
    private boolean cascade;
    private double consensus;

    private final int hc;

    public GenericIP(@NonNull String ip, boolean cascade, double consensus) {
        this.ip = ip;
        this.cascade = cascade;
        this.consensus = consensus;

        this.hc = Objects.hash(ip);
    }

    public @NonNull String getIp() { return ip; }

    public boolean getCascade() { return cascade; }

    public void setCascade(boolean status) { this.cascade = status; }

    public double getConsensus() { return consensus; }

    public void setConsensus(double status) { this.consensus = status; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericIP)) return false;
        GenericIP genericIP = (GenericIP) o;
        return ip.equals(genericIP.ip);
    }

    public int hashCode() { return hc; }

    public String toString() {
        return "GenericIP{" +
                "ip='" + ip + '\'' +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                '}';
    }
}
