package me.egg82.antivpn.core;

import java.util.Objects;
import java.util.UUID;

public class PostMCLeaksResult {
    private final long id;
    private final long longPlayerID;
    private final UUID playerID;
    private final boolean value;
    private final long created;

    private final int hc;

    public PostMCLeaksResult(long id, long longPlayerID, UUID playerID, boolean value, long created) {
        this.id = id;
        this.longPlayerID = longPlayerID;
        this.playerID = playerID;
        this.value = value;
        this.created = created;

        hc = Objects.hash(id);
    }

    public long getID() { return id; }

    public long getLongPlayerID() { return longPlayerID; }

    public UUID getPlayerID() { return playerID; }

    public boolean getValue() { return value; }

    public long getCreated() { return created; }

    public MCLeaksResult toMCLeaksResult() {
        return new MCLeaksResult(
                id,
                playerID,
                value,
                created
        );
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PostMCLeaksResult)) return false;
        PostMCLeaksResult that = (PostMCLeaksResult) o;
        return id == that.id;
    }

    public int hashCode() { return hc; }
}
