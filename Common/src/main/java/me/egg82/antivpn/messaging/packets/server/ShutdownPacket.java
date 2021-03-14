package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class ShutdownPacket extends AbstractPacket {
    private UUID server;

    public ShutdownPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public ShutdownPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public ShutdownPacket(@NotNull UUID server) {
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

    public void setServer(@NotNull UUID server) {
        this.server = server;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShutdownPacket)) {
            return false;
        }
        ShutdownPacket that = (ShutdownPacket) o;
        return server.equals(that.server);
    }

    public int hashCode() { return Objects.hash(server); }

    public String toString() {
        return "ShutdownPacket{" +
                "server=" + server +
                ", sender=" + sender +
                '}';
    }
}
