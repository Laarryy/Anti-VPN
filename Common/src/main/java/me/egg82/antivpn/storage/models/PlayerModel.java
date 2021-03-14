package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "avpn_6_player")
public class PlayerModel extends BaseModel {
    @Index(unique = true)
    @io.ebean.annotation.NotNull
    private UUID uuid;
    @io.ebean.annotation.NotNull
    private boolean mcleaks;

    public PlayerModel() {
        super();
        this.uuid = UUIDUtil.EMPTY_UUID;
        this.mcleaks = false;
    }

    public PlayerModel(@NotNull String dbName) {
        super(dbName);
        this.uuid = UUIDUtil.EMPTY_UUID;
        this.mcleaks = false;
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isMcleaks() { return mcleaks; }

    public void setMcleaks(boolean mcleaks) {
        this.mcleaks = mcleaks;
    }

    @Override
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
