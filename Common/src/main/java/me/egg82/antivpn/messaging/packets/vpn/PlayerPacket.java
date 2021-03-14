package me.egg82.antivpn.messaging.packets.vpn;

import io.netty.buffer.ByteBuf;

import java.util.Objects;
import java.util.UUID;

import me.egg82.antivpn.messaging.packets.AbstractPacket;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

public class PlayerPacket extends AbstractPacket {
    private UUID uuid;
    private boolean value;

    public PlayerPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public PlayerPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    public PlayerPacket(@NotNull UUID uuid, boolean value) {
        super(UUIDUtil.EMPTY_UUID);
        this.uuid = uuid;
        this.value = value;
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.uuid = readUUID(buffer);
        this.value = buffer.readBoolean();
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        writeUUID(this.uuid, buffer);
        buffer.writeBoolean(this.value);
    }

    public @NotNull UUID getUuid() { return uuid; }

    public void setUuid(@NotNull UUID uuid) {
        this.uuid = uuid;
    }

    public boolean getValue() { return value; }

    public void setValue(boolean value) {
        this.value = value;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerPacket)) {
            return false;
        }
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
