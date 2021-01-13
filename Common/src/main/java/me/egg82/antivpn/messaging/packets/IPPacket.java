package me.egg82.antivpn.messaging.packets;

import java.util.Objects;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class IPPacket extends AbstractPacket {
    private String ip;
    private int type;
    private Boolean cascade;
    private Double consensus;

    public byte getPacketId() { return 0x01; }

    public IPPacket(@NonNull ByteBuf data) { read(data); }

    public IPPacket() {
        this.ip = "";
        this.type = -1;
        this.cascade = null;
        this.consensus = null;
    }

    public void read(@NonNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.ip = readString(buffer);
        this.type = readVarInt(buffer);
        AlgorithmMethod method = AlgorithmMethod.values()[type];
        if (method == AlgorithmMethod.CASCADE) {
            this.cascade = buffer.readBoolean();
        } else {
            this.consensus = buffer.readDouble();
        }

        checkReadPacket(buffer);
    }

    public void write(@NonNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeString(this.ip, buffer);
        writeVarInt(this.type, buffer);
        AlgorithmMethod method = AlgorithmMethod.values()[type];
        if (method == AlgorithmMethod.CASCADE) {
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

    public @NonNull String getIp() { return ip; }

    public void setIp(@NonNull String ip) { this.ip = ip; }

    public @Nullable Boolean getCascade() { return cascade; }

    public void setCascade(Boolean cascade) { this.cascade = cascade; }

    public @Nullable Double getConsensus() { return consensus; }

    public void setConsensus(Double consensus) { this.consensus = consensus; }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPPacket)) return false;
        IPPacket ipPacket = (IPPacket) o;
        return type == ipPacket.type && Objects.equals(ip, ipPacket.ip) && Objects.equals(cascade, ipPacket.cascade) && Objects.equals(consensus, ipPacket.consensus);
    }

    public int hashCode() { return Objects.hash(ip, type, cascade, consensus); }

    public String toString() {
        return "IPPacket{" +
                "ip='" + ip + '\'' +
                ", type=" + type +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                '}';
    }
}
