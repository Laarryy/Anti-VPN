package me.egg82.antivpn.api.model.player;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import me.egg82.antivpn.utils.ExceptionUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPlayer implements Player {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    private final UUID uuid;
    private final String name;
    private boolean mcleaks;

    private final int hc;

    protected AbstractPlayer(@NonNull UUID uuid, String name, boolean mcleaks) {
        this.uuid = uuid;
        if (name == null) {
            try {
                name = fetchName(uuid).get();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
            }
        }
        this.name = name;
        this.mcleaks = mcleaks;

        this.hc = Objects.hash(uuid);
    }

    public @NonNull UUID getUuid() { return uuid; }

    public @Nullable String getName() { return name; }

    public boolean isMcLeaks() { return mcleaks; }

    public void setMcLeaks(boolean status) { this.mcleaks = status; }

    protected abstract @NonNull CompletableFuture<String> fetchName(@NonNull UUID uuid);

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
