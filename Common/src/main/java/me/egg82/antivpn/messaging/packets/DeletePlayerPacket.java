package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class DeletePlayerPacket extends AbstractPacket {
    private UUID uuid;

    public byte getPacketId() { return 0x04; }

    public DeletePlayerPacket(@NotNull ByteBuf data) { read(data); }

    public DeletePlayerPacket() {
        this.uuid = new UUID(0L, 0L);
    }

    public void read(@NotNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.uuid = readUUID(buffer);

        checkReadPacket(buffer);
    }

    public void write(@NotNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeUUID(this.uuid, buffer);
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) { this.uuid = uuid; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeletePlayerPacket)) return false;
        DeletePlayerPacket that = (DeletePlayerPacket) o;
        return uuid.equals(that.uuid);
    }

    public int hashCode() { return Objects.hash(uuid); }

    public String toString() {
        return "DeletePlayerPacket{" +
            "uuid=" + uuid +
            '}';
    }
}
