package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import io.ebean.annotation.Length;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "avpn_6_ip")
public class IPModel extends BaseModel {
    @Index(unique = true)
    @io.ebean.annotation.NotNull
    @Length(45)
    private String ip;
    @io.ebean.annotation.NotNull
    private int type;
    private Boolean cascade;
    private Double consensus;

    public IPModel() {
        super();
        this.ip = "";
        this.type = -1;
        this.cascade = null;
        this.consensus = null;
    }

    public IPModel(String dbName) {
        super(dbName);
        this.ip = "";
        this.type = -1;
        this.cascade = null;
        this.consensus = null;
    }

    public @NotNull String getIp() { return ip; }

    public void setIp(@NotNull String ip) {
        this.ip = ip;
    }

    public int getType() { return type; }

    public void setType(int type) {
        this.type = type;
    }

    public @Nullable Boolean getCascade() { return cascade; }

    public void setCascade(@Nullable Boolean cascade) {
        this.cascade = cascade;
    }

    public @Nullable Double getConsensus() { return consensus; }

    public void setConsensus(@Nullable Double consensus) {
        this.consensus = consensus;
    }

    public String toString() {
        return "IPModel{" +
                "id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                ", ip='" + ip + '\'' +
                ", type=" + type +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                '}';
    }
}
