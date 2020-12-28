package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;

public class IPPacket extends AbstractPacket {
    private String ip;
    private int type;
    private boolean cascade;
    private double consensus;

    public byte getPacketId() { return 0x01; }

    public IPPacket(ByteBuffer data) { read(data); }

    public IPPacket() {
        this.ip = null;
        this.type = -1;
        this.cascade = false;
        this.consensus = -1.0d;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.ip = getString(buffer);
        this.type = getVarInt(buffer);
        AlgorithmMethod method = AlgorithmMethod.values()[type];
        if (method == AlgorithmMethod.CASCADE) {
            this.cascade = getBoolean(buffer);
        } else {
            this.consensus = buffer.getDouble();
        }

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        putString(this.ip, buffer);
        putVarInt(this.type, buffer);
        AlgorithmMethod method = AlgorithmMethod.values()[type];
        if (method == AlgorithmMethod.CASCADE) {
            putBoolean(this.cascade, buffer);
        } else {
            buffer.putDouble(this.consensus);
        }
    }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public boolean getCascade() { return cascade; }

    public void setCascade(boolean cascade) { this.cascade = cascade; }

    public double getConsensus() { return consensus; }

    public void setConsensus(double consensus) { this.consensus = consensus; }

    public int getType() { return type; }

    public void setType(int type) { this.type = type; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPPacket)) return false;
        IPPacket ipPacket = (IPPacket) o;
        return type == ipPacket.type && cascade == ipPacket.cascade && Double.compare(ipPacket.consensus, consensus) == 0 && Objects.equals(ip, ipPacket.ip);
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
