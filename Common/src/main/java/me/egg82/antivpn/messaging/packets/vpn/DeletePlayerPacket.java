package me.egg82.antivpn.messaging.packets.vpn;

import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class DeletePlayerPacket extends AbstractPacket {
    private UUID uuid;

    public DeletePlayerPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public DeletePlayerPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public DeletePlayerPacket(@NotNull UUID uuid) {
        super(UUIDUtil.EMPTY_UUID);
        this.uuid = uuid;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.uuid = readUUID(buffer);
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.uuid, buffer);
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeletePlayerPacket)) {
            return false;
        }
        DeletePlayerPacket that = (DeletePlayerPacket) o;
        return uuid.equals(that.uuid);
    }

    public int hashCode() { return Objects.hash(uuid); }

    public String toString() {
        return "DeletePlayerPacket{" +
                "sender=" + sender +
                ", uuid=" + uuid +
                '}';
    }
}
