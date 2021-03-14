package me.egg82.antivpn.api.model.player;

import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public abstract class AbstractPlayer implements Player {
    protected final transient Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    private final UUID uuid;
    private final String name;
    private boolean mcleaks;

    private final int hc;

    protected AbstractPlayer(@NotNull UUID uuid, String name, boolean mcleaks) {
        this.uuid = uuid;
        if (name == null) {
            try {
                name = fetchName(uuid).get();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }
        }
        this.name = name;
        this.mcleaks = mcleaks;

        this.hc = Objects.hash(uuid);
    }

    public @NotNull UUID getUuid() { return uuid; }

    public @Nullable String getName() { return name; }

    public boolean isMcLeaks() { return mcleaks; }

    public void setMcLeaks(boolean status) { this.mcleaks = status; }

    protected abstract @NotNull CompletableFuture<@NotNull String> fetchName(@NotNull UUID uuid);

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
