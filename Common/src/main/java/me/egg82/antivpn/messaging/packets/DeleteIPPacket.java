package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.NonNull;

public class DeleteIPPacket extends AbstractPacket {
    private String ip;

    public byte getPacketId() { return 0x03; }

    public DeleteIPPacket(@NonNull ByteBuf data) { read(data); }

    public DeleteIPPacket() {
        this.ip = "";
    }

    public void read(@NonNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.ip = readString(buffer);

        checkReadPacket(buffer);
    }

    public void write(@NonNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeString(this.ip, buffer);
    }

    public @NonNull String getIp() { return ip; }

    public void setIp(@NonNull String ip) { this.ip = ip; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteIPPacket)) return false;
        DeleteIPPacket that = (DeleteIPPacket) o;
        return ip.equals(that.ip);
    }

    public int hashCode() { return Objects.hash(ip); }

    public String toString() {
        return "DeleteIPPacket{" +
            "ip='" + ip + '\'' +
            '}';
    }
}
