package me.egg82.antivpn.api.model.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.Objects;

public class GenericIP implements IP {
    private final InetAddress ip;
    private AlgorithmMethod type;
    private Boolean cascade;
    private Double consensus;

    private final int hc;

    public GenericIP(@NotNull InetAddress ip, @NotNull AlgorithmMethod type, @Nullable Boolean cascade, @Nullable Double consensus) {
        this.ip = ip;
        this.type = type;
        this.cascade = cascade;
        this.consensus = consensus;

        this.hc = Objects.hash(ip);
    }

    @Override
    public @NotNull InetAddress getIP() { return ip; }

    @Override
    public @Nullable Boolean getCascade() { return cascade; }

    @Override
    public void setCascade(@Nullable Boolean status) {
        this.cascade = status;
    }

    @Override
    public @Nullable Double getConsensus() { return consensus; }

    @Override
    public void setConsensus(@Nullable Double status) {
        this.consensus = status;
    }

    @Override
    public @NotNull AlgorithmMethod getType() { return type; }

    @Override
    public void setType(@NotNull AlgorithmMethod type) {
        this.type = type;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GenericIP)) {
            return false;
        }
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
