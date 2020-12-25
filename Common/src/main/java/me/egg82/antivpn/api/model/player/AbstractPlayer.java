package me.egg82.antivpn.api.model.player;

import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractPlayer implements Player {
    private final UUID uuid;
    private final String name;
    private boolean mcleaks;

    private final int hc;

    protected AbstractPlayer(@NonNull UUID uuid, String name, boolean mcleaks) {
        this.uuid = uuid;
        this.name = name == null ? fetchName(uuid) : name;
        this.mcleaks = mcleaks;

        this.hc = Objects.hash(uuid);
    }

    public @NonNull UUID getUuid() { return uuid; }

    public @Nullable String getName() { return name; }

    public boolean isMcLeaks() { return mcleaks; }

    public void setMcLeaks(boolean status) { this.mcleaks = status; }

    protected abstract @Nullable String fetchName(@NonNull UUID uuid);

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractPlayer)) return false;
        AbstractPlayer that = (AbstractPlayer) o;
        return uuid.equals(that.uuid);
    }

    public int hashCode() { return hc; }

    public String toString() {
        return "AbstractPlayer{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", mcleaks=" + mcleaks +
                '}';
    }
}
