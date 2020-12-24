package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;

public class PlayerPacket extends AbstractPacket {
    private long id;
    private UUID uuid;

    public byte getPacketId() { return 0x02; }

    public PlayerPacket(ByteBuffer data) { read(data); }

    public PlayerPacket() {
        this.id = -1L;
        this.uuid = null;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.id = buffer.getLong();
        this.uuid = getUUID(buffer);

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        buffer.putLong(this.id);
        putUUID(this.uuid, buffer);
    }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public UUID getUuid() { return uuid; }

    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPacket)) return false;
        PlayerPacket that = (PlayerPacket) o;
        return id == that.id && Objects.equals(uuid, that.uuid);
    }

    public int hashCode() { return Objects.hash(id, uuid); }

    public String toString() {
        return "PlayerPacket{" +
                "id=" + id +
                ", uuid=" + uuid +
                '}';
    }
}
