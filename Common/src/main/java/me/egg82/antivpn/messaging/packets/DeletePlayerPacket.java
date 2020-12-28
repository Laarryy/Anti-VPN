package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public class DeletePlayerPacket extends AbstractPacket {
    private UUID uuid;

    public byte getPacketId() { return 0x04; }

    public DeletePlayerPacket(ByteBuffer data) { read(data); }

    public DeletePlayerPacket() {
        this.uuid = null;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.uuid = getUUID(buffer);

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        putUUID(this.uuid, buffer);
    }

    public UUID getUuid() { return uuid; }

    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeletePlayerPacket)) return false;
        DeletePlayerPacket that = (DeletePlayerPacket) o;
        return Objects.equals(uuid, that.uuid);
    }

    public int hashCode() { return Objects.hash(uuid); }

    public String toString() {
        return "DeletePlayerPacket{" +
                "uuid=" + uuid +
                '}';
    }
}
