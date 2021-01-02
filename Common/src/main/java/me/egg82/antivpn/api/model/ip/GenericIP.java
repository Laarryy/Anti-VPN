package me.egg82.antivpn.api.model.ip;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericIP implements IP {
    private final String ip;
    private AlgorithmMethod type;
    private Boolean cascade;
    private Double consensus;

    private final int hc;

    public GenericIP(@NonNull String ip, @NonNull AlgorithmMethod type, @Nullable Boolean cascade, @Nullable Double consensus) {
        this.ip = ip;
        this.type = type;
        this.cascade = cascade;
        this.consensus = consensus;

        this.hc = Objects.hash(ip);
    }

    public @NonNull String getIp() { return ip; }

    public @Nullable Boolean getCascade() { return cascade; }

    public void setCascade(@Nullable Boolean status) { this.cascade = status; }

    public @Nullable Double getConsensus() { return consensus; }

    public void setConsensus(@Nullable Double status) { this.consensus = status; }

    public @NonNull AlgorithmMethod getType() { return type; }

    public void setType(@NonNull AlgorithmMethod type) { this.type = type; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericIP)) return false;
        GenericIP genericIP = (GenericIP) o;
        return ip.equals(genericIP.ip);
    }

    public int hashCode() { return hc; }

    public String toString() {
        return "GenericIP{" +
                "ip='" + ip + '\'' +
                ", type=" + type +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                '}';
    }
}
