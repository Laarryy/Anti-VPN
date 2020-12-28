package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;

public class DeleteIPPacket extends AbstractPacket {
    private String ip;

    public byte getPacketId() { return 0x03; }

    public DeleteIPPacket(ByteBuffer data) { read(data); }

    public DeleteIPPacket() {
        this.ip = null;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.ip = getString(buffer);

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        putString(this.ip, buffer);
    }

    public String getIp() { return ip; }

    public void setIp(String ip) { this.ip = ip; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteIPPacket)) return false;
        DeleteIPPacket that = (DeleteIPPacket) o;
        return Objects.equals(ip, that.ip);
    }

    public int hashCode() { return Objects.hash(ip); }

    public String toString() {
        return "DeleteIPPacket{" +
                "ip='" + ip + '\'' +
                '}';
    }
}
