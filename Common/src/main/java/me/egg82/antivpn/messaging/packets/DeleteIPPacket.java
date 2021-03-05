package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class DeleteIPPacket extends AbstractPacket {
    private String ip;

    public byte getPacketId() { return (byte) 3; }

    public DeleteIPPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public DeleteIPPacket() {
        super(new UUID(0L, 0L));
    }

    public DeleteIPPacket(@NotNull String ip) {
        super(new UUID(0L, 0L));
        this.ip = ip;
    }

    public void read(@NotNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.ip = readString(buffer);

        checkReadPacket(buffer);
    }

    public void write(@NotNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeString(this.ip, buffer);
    }

    public @NotNull String getIp() { return ip; }

    public void setIp(@NotNull String ip) { this.ip = ip; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteIPPacket)) return false;
        DeleteIPPacket that = (DeleteIPPacket) o;
        return ip.equals(that.ip);
    }

    public int hashCode() { return Objects.hash(ip); }

    public String toString() {
        return "DeleteIPPacket{" +
            "sender=" + sender +
            ", ip='" + ip + '\'' +
            '}';
    }
}
