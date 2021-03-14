package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.UUID;

import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

public class PacketVersionPacket extends AbstractPacket {
    private UUID intendedRecipient;
    private UUID server;
    private byte packetVersion;

    public PacketVersionPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public PacketVersionPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public PacketVersionPacket(@NotNull UUID intendedRecipient, @NotNull UUID server, byte protocolVersion) {
        super(UUIDUtil.EMPTY_UUID);
        this.intendedRecipient = intendedRecipient;
        this.server = server;
        this.packetVersion = protocolVersion;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.intendedRecipient = readUUID(buffer);
        this.server = readUUID(buffer);
        this.packetVersion = buffer.readByte();
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.intendedRecipient, buffer);
        writeUUID(this.server, buffer);
        buffer.writeByte(this.packetVersion);
    }

    public @NotNull UUID getIntendedRecipient() { return intendedRecipient; }

    public void setIntendedRecipient(@NotNull UUID intendedRecipient) {
        this.intendedRecipient = intendedRecipient;
    }

    public @NotNull UUID getServer() { return server; }

    public void setServer(@NotNull UUID server) {
        this.server = server;
    }

    public byte getPacketVersion() { return packetVersion; }

    public void setPacketVersion(byte packetVersion) {
        this.packetVersion = packetVersion;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PacketVersionPacket)) {
            return false;
        }
        PacketVersionPacket that = (PacketVersionPacket) o;
        return packetVersion == that.packetVersion && intendedRecipient.equals(that.intendedRecipient) && server.equals(that.server);
    }

    public int hashCode() { return Objects.hash(intendedRecipient, server, packetVersion); }

    public String toString() {
        return "PacketVersionPacket{" +
                "sender=" + sender +
                ", intendedRecipient=" + intendedRecipient +
                ", server=" + server +
                ", packetVersion=" + packetVersion +
                '}';
    }
}
