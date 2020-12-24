package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import io.ebean.annotation.NotNull;
import java.util.UUID;
import javax.persistence.Entity;

@Entity(name = "avpn_player")
public class PlayerModel extends BaseModel {
    @Index(unique = true) @NotNull
    private UUID uuid;
    private boolean mcleaks;

    public PlayerModel() {
        super();
        this.uuid = null;
        this.mcleaks = false;
    }

    public PlayerModel(String dbName) {
        super(dbName);
        this.uuid = null;
        this.mcleaks = false;
    }

    public UUID getUuid() { return uuid; }

    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public boolean isMcleaks() { return mcleaks; }

    public void setMcleaks(boolean mcleaks) { this.mcleaks = mcleaks; }

    public String toString() {
        return "PlayerModel{" +
                "uuid=" + uuid +
                ", mcleaks=" + mcleaks +
                ", id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                '}';
    }
}
