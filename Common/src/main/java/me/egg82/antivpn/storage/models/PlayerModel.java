package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import io.ebean.annotation.NotNull;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.checkerframework.checker.nullness.qual.NonNull;

@Entity
@Table(name = "avpn_player")
public class PlayerModel extends BaseModel {
    @Index(unique = true) @NotNull
    private UUID uuid;
    private boolean mcleaks;

    public PlayerModel() {
        super();
        this.uuid = new UUID(0L, 0L);
        this.mcleaks = false;
    }

    public PlayerModel(@NonNull String dbName) {
        super(dbName);
        this.uuid = new UUID(0L, 0L);
        this.mcleaks = false;
    }

    public @NonNull UUID getUuid() { return uuid; }

    public void setUuid(@NonNull UUID uuid) { this.uuid = uuid; }

    public boolean isMcleaks() { return mcleaks; }

    public void setMcleaks(boolean mcleaks) { this.mcleaks = mcleaks; }

    public String toString() {
        return "PlayerModel{" +
                "id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                ", uuid=" + uuid +
                ", mcleaks=" + mcleaks +
                '}';
    }
}
