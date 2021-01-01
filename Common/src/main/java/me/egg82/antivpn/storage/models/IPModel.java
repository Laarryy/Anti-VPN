package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import io.ebean.annotation.Length;
import io.ebean.annotation.NotNull;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.checkerframework.checker.nullness.qual.NonNull;

@Entity
@Table(name = "avpn_ip")
public class IPModel extends BaseModel {
    @Index(unique = true) @NotNull @Length(45)
    private String ip;
    private int type;
    private boolean cascade;
    private double consensus;

    public IPModel() {
        super();
        this.ip = "";
        this.type = -1;
        this.cascade = false;
        this.consensus = Double.NaN;
    }

    public IPModel(String dbName) {
        super(dbName);
        this.ip = "";
        this.type = -1;
        this.cascade = false;
        this.consensus = Double.NaN;
    }

    public @NonNull String getIp() { return ip; }

    public void setIp(@NonNull String ip) { this.ip = ip; }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public boolean getCascade() { return cascade; }

    public void setCascade(boolean cascade) { this.cascade = cascade; }

    public double getConsensus() { return consensus; }

    public void setConsensus(double consensus) { this.consensus = consensus; }

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
