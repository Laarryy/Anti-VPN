package me.egg82.antivpn.core;

import java.util.Objects;

public class RawMCLeaksResult {
    private final long id;
    private final long longPlayerID;
    private final boolean value;
    private final long created;

    private final int hc;

    public RawMCLeaksResult(long id, long longPlayerID, boolean value, long created) {
        this.id = id;
        this.longPlayerID = longPlayerID;
        this.value = value;
        this.created = created;

        hc = Objects.hash(id);
    }

    public long getID() { return id; }

    public long getLongPlayerID() { return longPlayerID; }

    public boolean getValue() { return value; }

    public long getCreated() { return created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RawMCLeaksResult)) return false;
        RawMCLeaksResult that = (RawMCLeaksResult) o;
        return id == that.id;
    }

    public int hashCode() { return hc; }
}
