package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.Objects;

public class MCLeaksPacket extends AbstractPacket {
    private long id;
    private boolean value;
    private long created;

    public byte getPacketId() { return 0x04; }

    public MCLeaksPacket(ByteBuffer data) { read(data); }

    public MCLeaksPacket() {
        this.id = -1L;
        this.value = false;
        this.created = -1L;
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.id = buffer.getLong();
        this.value = getBoolean(buffer);
        this.created = buffer.getLong();

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        buffer.putLong(this.id);
        putBoolean(this.value, buffer);
        buffer.putLong(created);
    }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public boolean getValue() { return value; }

    public void setValue(boolean value) { this.value = value; }

    public long getCreated() { return created; }

    public void setCreated(long created) { this.created = created; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MCLeaksPacket)) return false;
        MCLeaksPacket that = (MCLeaksPacket) o;
        return id == that.id && value == that.value && created == that.created;
    }

    public int hashCode() { return Objects.hash(id, value, created); }

    public String toString() {
        return "MCLeaksPacket{" +
                "id=" + id +
                ", value=" + value +
                ", created=" + created +
                '}';
    }
}
