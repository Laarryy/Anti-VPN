package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerPacket extends AbstractPacket {
    private UUID uuid;
    private boolean value;

    public byte getPacketId() { return 0x02; }

    public PlayerPacket(@NonNull ByteBuffer data) { read(data); }

    public PlayerPacket() {
        this.uuid = null;
        this.value = false;
    }

    public void read(@NonNull ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.uuid = getUUID(buffer);
        this.value = getBoolean(buffer);

        checkReadPacket(buffer);
    }

    public void write(@NonNull ByteBuffer buffer) {
        buffer.put(VERSION);

        putUUID(this.uuid, buffer);
        putBoolean(this.value, buffer);
    }

    public @NonNull UUID getUuid() { return uuid; }

    public void setUuid(@NonNull UUID uuid) { this.uuid = uuid; }

    public boolean getValue() { return value; }

    public void setValue(boolean value) { this.value = value; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerPacket)) return false;
        PlayerPacket that = (PlayerPacket) o;
        return value == that.value && uuid.equals(that.uuid);
    }

    public int hashCode() { return Objects.hash(uuid, value); }

    public String toString() {
        return "PlayerPacket{" +
                "uuid=" + uuid +
                ", value=" + value +
                '}';
    }
}
