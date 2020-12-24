package me.egg82.antivpn.storage.models;

import io.ebean.annotation.Index;
import io.ebean.annotation.Length;
import io.ebean.annotation.NotNull;
import javax.persistence.Entity;

@Entity(name = "avpn_ip")
public class IPModel extends BaseModel {
    @Index(unique = true) @NotNull @Length(45)
    private String ip;
    private boolean cascade;
    private double consensus;

    public IPModel() {
        super();
        this.ip = null;
        this.cascade = false;
        this.consensus = Double.NaN;
    }

    public IPModel(String dbName) {
        super(dbName);
        this.ip = null;
        this.cascade = false;
        this.consensus = Double.NaN;
    }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public boolean isCascade() { return cascade; }

    public void setCascade(boolean cascade) { this.cascade = cascade; }

    public double getConsensus() { return consensus; }

    public void setConsensus(double consensus) { this.consensus = consensus; }

    public String toString() {
        return "IPModel{" +
                "ip='" + ip + '\'' +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                ", id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                '}';
    }
}
