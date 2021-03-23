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
    protected final transient @NotNull Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    private final @NotNull UUID uuid;
    private final @Nullable String name;
    private boolean mcleaks;

    private final int hc;

    protected AbstractPlayer(@NotNull UUID uuid, @Nullable String name, boolean mcleaks) {
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

    @Override
    @NotNull
    public UUID getUuid() { return uuid; }

    @Override
    @Nullable
    public String getName() { return name; }

    @Override
    public boolean isMcLeaks() { return mcleaks; }

    @Override
    public void setMcLeaks(boolean status) {
        this.mcleaks = status;
    }

    @NotNull
    protected abstract CompletableFuture<@NotNull String> fetchName(@NotNull UUID uuid);

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractPlayer)) {
            return false;
        }
        AbstractPlayer that = (AbstractPlayer) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() { return hc; }

    @Override
    public String toString() {
        return "AbstractPlayer{" +
                "uuid=" + uuid +
                ", name='" + name + '\'' +
                ", mcleaks=" + mcleaks +
                '}';
    }
}
