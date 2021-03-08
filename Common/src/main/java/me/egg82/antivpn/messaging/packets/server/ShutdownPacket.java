package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import org.jetbrains.annotations.NotNull;

public class ShutdownPacket extends AbstractPacket {
    private UUID server;

    public ShutdownPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public ShutdownPacket() {
        super(new UUID(0L, 0L));
    }

    public ShutdownPacket(@NotNull UUID server) {
        super(new UUID(0L, 0L));
        this.server = server;
    }

    public void read(@NotNull ByteBuf buffer) {
        this.server = readUUID(buffer);
    }

    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.server, buffer);
    }

    public @NotNull UUID getServer() { return server; }

    public void setServer(@NotNull UUID server) { this.server = server; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShutdownPacket)) return false;
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
