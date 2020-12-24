package me.egg82.antivpn.storage.results;

import java.util.Objects;
import java.util.UUID;

public class MCLeaksResult {
    private final long id;
    private final UUID playerID;
    private final boolean value;
    private final long created;

    private final int hc;

    public MCLeaksResult(long id, UUID playerID, boolean value, long created) {
        this.id = id;
        this.playerID = playerID;
        this.value = value;
        this.created = created;

        hc = Objects.hash(id);
    }

    public long getID() { return id; }

    public UUID getPlayerID() { return playerID; }

    public boolean getValue() { return value; }

    public long getCreated() { return created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MCLeaksResult)) return false;
        MCLeaksResult that = (MCLeaksResult) o;
        return id == that.id;
    }

    public int hashCode() { return hc; }
}
