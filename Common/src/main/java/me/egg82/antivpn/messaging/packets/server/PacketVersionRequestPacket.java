package me.egg82.antivpn.messaging.packets.server;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class PacketVersionRequestPacket extends AbstractPacket {
    private UUID intendedRecipient;
    private UUID server;

    public PacketVersionRequestPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public PacketVersionRequestPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public PacketVersionRequestPacket(@NotNull UUID intendedRecipient, @NotNull UUID server) {
        super(UUIDUtil.EMPTY_UUID);
        this.intendedRecipient = intendedRecipient;
        this.server = server;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.intendedRecipient = readUUID(buffer);
        this.server = readUUID(buffer);
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.intendedRecipient, buffer);
        writeUUID(this.server, buffer);
    }

    public @NotNull UUID getIntendedRecipient() { return intendedRecipient; }

    public void setIntendedRecipient(@NotNull UUID intendedRecipient) { this.intendedRecipient = intendedRecipient; }

    public @NotNull UUID getServer() { return server; }

    public void setServer(@NotNull UUID server) { this.server = server; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PacketVersionRequestPacket)) {
            return false;
        }
        PacketVersionRequestPacket that = (PacketVersionRequestPacket) o;
        return intendedRecipient.equals(that.intendedRecipient) && server.equals(that.server);
    }

    @Override
    public int hashCode() { return Objects.hash(intendedRecipient, server); }

    @Override
    public String toString() {
        return "PacketVersionRequestPacket{" +
                "sender=" + sender +
                ", intendedRecipient=" + intendedRecipient +
                ", server=" + server +
                '}';
    }
}
