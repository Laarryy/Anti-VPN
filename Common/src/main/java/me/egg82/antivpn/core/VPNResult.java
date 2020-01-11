package me.egg82.antivpn.core;

import java.util.Objects;
import java.util.Optional;

public class VPNResult {
    private final long id;
    private final String ip;
    private final Optional<Boolean> cascade;
    private final Optional<Double> consensus;
    private final long created;

    private final int hc;

    public VPNResult(long id, String ip, Optional<Boolean> cascade, Optional<Double> consensus, long created) {
        this.id = id;
        this.ip = ip;
        this.cascade = cascade;
        this.consensus = consensus;
        this.created = created;

        hc = Objects.hash(id);
    }

    public long getID() { return id; }

    public String getIP() { return ip; }

    public Optional<Boolean> getCascade() { return cascade; }

    public Optional<Double> getConsensus() { return consensus; }

    public long getCreated() { return created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VPNResult)) return false;
        VPNResult that = (VPNResult) o;
        return id == that.id;
    }

    public int hashCode() { return hc; }
}
