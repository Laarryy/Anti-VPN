package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;

public class IPPacket extends AbstractPacket {
    private long id;
    private String ip;

    public byte getPacketId() { return 0x01; }

    public IPPacket(ByteBuffer data) { read(data); }

    public IPPacket() {
        this.id = -1L;
        this.ip = null;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.id = buffer.getLong();
        this.ip = getString(buffer);

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        buffer.putLong(this.id);
        putString(this.ip, buffer);
    }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IPPacket)) return false;
        IPPacket ipPacket = (IPPacket) o;
        return id == ipPacket.id && Objects.equals(ip, ipPacket.ip);
    }

    public int hashCode() { return Objects.hash(id, ip); }

    public String toString() {
        return "IPPacket{" +
                "id=" + id +
                ", ip='" + ip + '\'' +
                '}';
    }
}
