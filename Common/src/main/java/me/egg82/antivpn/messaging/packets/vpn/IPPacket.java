package me.egg82.antivpn.messaging.packets.vpn;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class IPPacket extends AbstractPacket {
    private String ip;
    private AlgorithmMethod type;
    private Boolean cascade;
    private Double consensus;

    public IPPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public IPPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public IPPacket(@NotNull String ip, @Nullable Boolean cascade) {
        super(UUIDUtil.EMPTY_UUID);
        this.ip = ip;
        this.type = AlgorithmMethod.CASCADE;
        this.cascade = cascade;
        this.consensus = null;
    }

    public IPPacket(@NotNull String ip, @Nullable Double consensus) {
        super(UUIDUtil.EMPTY_UUID);
        this.ip = ip;
        this.type = AlgorithmMethod.CONSESNSUS;
        this.cascade = null;
        this.consensus = consensus;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.ip = readString(buffer);
        AlgorithmMethod method = AlgorithmMethod.values()[readVarInt(buffer)];
        if (method == AlgorithmMethod.CASCADE) {
            this.cascade = buffer.readBoolean();
        } else {
            this.consensus = buffer.readDouble();
        }
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeString(this.ip, buffer);
        writeVarInt(this.type.ordinal(), buffer);
        if (this.type == AlgorithmMethod.CASCADE) {
            if (this.cascade == null) {
                throw new RuntimeException("cascade was selected as type but value is null.");
            }
            buffer.writeBoolean(this.cascade);
        } else {
            if (this.consensus == null) {
                throw new RuntimeException("consensus was selected as type but value is null.");
            }
            buffer.writeDouble(this.consensus);
        }
    }

    public @NotNull String getIp() { return ip; }

    public void setIp(@NotNull String ip) {
        this.ip = ip;
    }

    public @Nullable Boolean getCascade() { return cascade; }

    public void setCascade(@Nullable Boolean cascade) {
        this.cascade = cascade;
    }

    public @Nullable Double getConsensus() { return consensus; }

    public void setConsensus(@Nullable Double consensus) {
        this.consensus = consensus;
    }

    public @NotNull AlgorithmMethod getType() { return type; }

    public void setType(@NotNull AlgorithmMethod type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IPPacket)) {
            return false;
        }
        IPPacket ipPacket = (IPPacket) o;
        return ip.equals(ipPacket.ip) && type == ipPacket.type && Objects.equals(cascade, ipPacket.cascade) && Objects.equals(consensus, ipPacket.consensus);
    }

    @Override
    public int hashCode() { return Objects.hash(ip, type, cascade, consensus); }

    @Override
    public String toString() {
        return "IPPacket{" +
                "sender=" + sender +
                ", ip='" + ip + '\'' +
                ", type=" + type +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                '}';
    }
}
