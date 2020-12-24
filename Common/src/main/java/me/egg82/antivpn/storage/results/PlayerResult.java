package me.egg82.antivpn.storage.results;

import java.util.Objects;
import java.util.UUID;

public class PlayerResult {
    private final long longPlayerID;
    private final UUID playerID;

    private final int hc;

    public PlayerResult(long longPlayerID, UUID playerID) {
        this.longPlayerID = longPlayerID;
        this.playerID = playerID;

        hc = Objects.hash(longPlayerID);
    }

    public long getLongPlayerID() { return longPlayerID; }

    public UUID getPlayerID() { return playerID; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerResult)) return false;
        PlayerResult that = (PlayerResult) o;
        return longPlayerID == that.longPlayerID;
    }

    public int hashCode() { return hc; }
}
