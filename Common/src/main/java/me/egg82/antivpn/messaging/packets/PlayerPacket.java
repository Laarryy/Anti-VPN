package me.egg82.antivpn.messaging.packets;

import java.util.Objects;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.NonNull;

public class PlayerPacket extends AbstractPacket {
    private UUID uuid;
    private boolean value;

    public byte getPacketId() { return 0x02; }

    public PlayerPacket(@NonNull ByteBuf data) { read(data); }

    public PlayerPacket() {
        this.uuid = null;
        this.value = false;
    }

    public void read(@NonNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.uuid = readUUID(buffer);
        this.value = buffer.readBoolean();

        checkReadPacket(buffer);
    }

    public void write(@NonNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeUUID(this.uuid, buffer);
        buffer.writeBoolean(this.value);
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
