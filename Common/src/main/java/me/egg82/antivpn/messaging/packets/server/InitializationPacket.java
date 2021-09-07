package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class InitializationPacket extends AbstractPacket {
    private UUID server;
    private byte packetVersion;

    public InitializationPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public InitializationPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public InitializationPacket(@NotNull UUID server, byte protocolVersion) {
        super(UUIDUtil.EMPTY_UUID);
        this.server = server;
        this.packetVersion = protocolVersion;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.server = readUUID(buffer);
        this.packetVersion = buffer.readByte();
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.server, buffer);
        buffer.writeByte(this.packetVersion);
    }

    public @NotNull UUID getServer() { return server; }

    public void setServer(@NotNull UUID server) { this.server = server; }

    public byte getPacketVersion() { return packetVersion; }

    public void setPacketVersion(byte packetVersion) { this.packetVersion = packetVersion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InitializationPacket)) {
            return false;
        }
        InitializationPacket that = (InitializationPacket) o;
        return packetVersion == that.packetVersion && server.equals(that.server);
    }

    @Override
    public int hashCode() { return Objects.hash(server, packetVersion); }

    @Override
    public String toString() {
        return "InitializationPacket{" +
                "server=" + server +
                ", packetVersion=" + packetVersion +
                ", sender=" + sender +
                '}';
    }
}
