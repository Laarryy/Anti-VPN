package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.jetbrains.annotations.NotNull;

@Entity
@Table(name = "avpn_6_player")
public class PlayerModel extends BaseModel {
    @Index(unique = true) @io.ebean.annotation.NotNull
    private UUID uuid;
    @io.ebean.annotation.NotNull
    private boolean mcleaks;

    public PlayerModel() {
        super();
        this.uuid = new UUID(0L, 0L);
        this.mcleaks = false;
    }

    public PlayerModel(@NotNull String dbName) {
        super(dbName);
        this.uuid = new UUID(0L, 0L);
        this.mcleaks = false;
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) { this.uuid = uuid; }

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
