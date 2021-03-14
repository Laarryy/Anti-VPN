package me.egg82.antivpn.messaging.packets.vpn;

import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.UUID;

import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

public class DeleteIPPacket extends AbstractPacket {
    private String ip;

    public DeleteIPPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public DeleteIPPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public DeleteIPPacket(@NotNull String ip) {
        super(UUIDUtil.EMPTY_UUID);
        this.ip = ip;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.ip = readString(buffer);
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeString(this.ip, buffer);
    }

    public @NotNull String getIp() {
        return ip;
    }

    public void setIp(@NotNull String ip) {
        this.ip = ip;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeleteIPPacket)) {
            return false;
        }
        DeleteIPPacket that = (DeleteIPPacket) o;
        return ip.equals(that.ip);
    }

    public int hashCode() {
        return Objects.hash(ip);
    }

    public String toString() {
        return "DeleteIPPacket{" +
                "sender=" + sender +
                ", ip='" + ip + '\'' +
                '}';
    }
}
