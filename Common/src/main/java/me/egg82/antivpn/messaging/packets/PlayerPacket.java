package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

public class PlayerPacket extends AbstractPacket {
    private UUID uuid;
    private boolean value;

    public byte getPacketId() { return (byte) 2; }

    public PlayerPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public PlayerPacket() {
        super(new UUID(0L, 0L));
    }

    public PlayerPacket(@NotNull UUID uuid, boolean value) {
        super(new UUID(0L, 0L));
        this.uuid = uuid;
        this.value = value;
    }

    public void read(@NotNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.uuid = readUUID(buffer);
        this.value = buffer.readBoolean();

        checkReadPacket(buffer);
    }

    public void write(@NotNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        writeUUID(this.uuid, buffer);
        buffer.writeBoolean(this.value);
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) { this.uuid = uuid; }

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
            "sender=" + sender +
            ", uuid=" + uuid +
            ", value=" + value +
            '}';
    }
}
