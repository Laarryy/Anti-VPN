package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import me.egg82.antivpn.lang.LocaleUtil;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;

public class MultiPacket extends AbstractPacket {
    private static final ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;

    private Set<Packet> packets = new LinkedHashSet<>();

    public byte getPacketId() { return 0x21; }

    public MultiPacket(@NotNull ByteBuf data) { read(data); }

    public MultiPacket() { }

    public void read(@NotNull ByteBuf buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        this.packets.clear();

        byte nextPacket;
        while (buffer.readableBytes() > 0 && (nextPacket = buffer.readByte()) != 0x00) { // Seek end of multi-packet or end of buffer
            Class<Packet> packetClass = PacketUtil.getPacketCache().get(nextPacket);
            if (packetClass == null) {
                logger.warn("Got packet ID that doesn't exist: " + nextPacket);
                continue;
            }

            int packetLen = buffer.readInt();
            ByteBuf packetBuf = alloc.buffer(packetLen, packetLen);
            try {
                buffer.readBytes(packetBuf);
                try {
                    packets.add(packetClass.getConstructor(ByteBuf.class).newInstance(packetBuf));
                } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                    logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", packetClass.getSimpleName()), ex);
                }
            } finally {
                packetBuf.release();
            }
        }

        checkReadPacket(buffer);
    }

    public void write(@NotNull ByteBuf buffer) {
        buffer.writeByte(VERSION);

        if (packets.isEmpty()) {
            buffer.writeByte((byte) 0x00); // End of multi-packet
            return;
        }

        for (Packet packet : packets) {
            if (packet == null) {
                continue;
            }

            buffer.writeByte(packet.getPacketId()); // Write packet ID
            int start = buffer.writerIndex();
            buffer.writeInt(0); // Make room for an int at the head
            packet.write(buffer);
            buffer.setInt(start, buffer.writerIndex() - start - 4); // Write the packet length to the int at the head
        }

        buffer.writeByte((byte) 0x00); // End of multi-packet
    }

    public @NotNull Set<@NotNull Packet> getPackets() { return packets; }

    public void setPackets(@NotNull Set<@NotNull Packet> packets) { this.packets = packets; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiPacket)) return false;
        MultiPacket that = (MultiPacket) o;
        return packets.equals(that.packets);
    }

    public int hashCode() { return Objects.hash(packets); }

    public String toString() {
        return "MultiPacket{" +
            "packets=" + packets +
            '}';
    }
}
