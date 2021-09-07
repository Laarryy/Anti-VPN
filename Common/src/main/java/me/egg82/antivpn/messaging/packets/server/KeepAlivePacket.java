package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class KeepAlivePacket extends AbstractPacket {
    private UUID server;

    public KeepAlivePacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public KeepAlivePacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public KeepAlivePacket(@NotNull UUID server) {
        super(UUIDUtil.EMPTY_UUID);
        this.server = server;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.server = readUUID(buffer);
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.server, buffer);
    }

    public @NotNull UUID getServer() { return server; }

    public void setServer(@NotNull UUID server) { this.server = server; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof KeepAlivePacket)) {
            return false;
        }
        KeepAlivePacket that = (KeepAlivePacket) o;
        return server.equals(that.server);
    }

    @Override
    public int hashCode() { return Objects.hash(server); }

    @Override
    public String toString() {
        return "KeepAlivePacket{" +
                "server=" + server +
                ", sender=" + sender +
                '}';
    }
}
