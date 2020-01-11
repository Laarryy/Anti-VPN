package me.egg82.antivpn.core;

import java.util.Objects;
import java.util.Optional;

public class RawVPNResult {
    private final long id;
    private final long ipID;
    private final Optional<Boolean> cascade;
    private final Optional<Double> consensus;
    private final long created;

    private final int hc;

    public RawVPNResult(long id, long ipID, Optional<Boolean> cascade, Optional<Double> consensus, long created) {
        this.id = id;
        this.ipID = ipID;
        this.cascade = cascade;
        this.consensus = consensus;
        this.created = created;

        hc = Objects.hash(id);
    }

    public long getID() { return id; }

    public long getIPID() { return ipID; }

    public Optional<Boolean> getCascade() { return cascade; }

    public Optional<Double> getConsensus() { return consensus; }

    public long getCreated() { return created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawVPNResult)) return false;
        RawVPNResult that = (RawVPNResult) o;
        return id == that.id;
    }

    public int hashCode() { return hc; }
}
