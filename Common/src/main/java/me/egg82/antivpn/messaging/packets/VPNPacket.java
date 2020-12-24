package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;

public class VPNPacket extends AbstractPacket {
    private long id;
    private boolean cascade;
    private double consensus;
    private long created;

    public byte getPacketId() { return 0x03; }

    public VPNPacket(ByteBuffer data) { read(data); }

    public VPNPacket() {
        this.id = -1L;
        this.cascade = false;
        this.consensus = -1.0d;
        this.created = -1L;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.id = buffer.getLong();
        if (getBoolean(buffer)) {
            this.cascade = getBoolean(buffer);
        } else {
            this.consensus = buffer.getDouble();
        }
        this.created = buffer.getLong();

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        buffer.putLong(this.id);
        putBoolean(consensus <= -1.0d, buffer);
        if (consensus <= -1.0d) {
            putBoolean(this.cascade, buffer);
        } else {
            buffer.putDouble(consensus);
        }
        buffer.putLong(created);
    }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public boolean isCascade() { return consensus <= -1.0d; }

    public boolean getCascade() { return cascade; }

    public void setCascade(boolean cascade) { this.cascade = cascade; }

    public double getConsensus() { return consensus; }

    public void setConsensus(double consensus) { this.consensus = consensus; }

    public long getCreated() { return created; }

    public void setCreated(long created) { this.created = created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VPNPacket)) return false;
        VPNPacket vpnPacket = (VPNPacket) o;
        return id == vpnPacket.id && cascade == vpnPacket.cascade && Double.compare(vpnPacket.consensus, consensus) == 0 && created == vpnPacket.created;
    }

    public int hashCode() { return Objects.hash(id, cascade, consensus, created); }

    public String toString() {
        return "VPNPacket{" +
                "id=" + id +
                ", cascade=" + cascade +
                ", consensus=" + consensus +
                ", created=" + created +
                '}';
    }
}
