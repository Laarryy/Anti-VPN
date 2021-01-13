package me.egg82.antivpn.messaging.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import me.egg82.antivpn.utils.PacketUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class MultiPacket extends AbstractPacket {
    private static final ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;

    private List<Packet> packets;

    public byte getPacketId() { return 0x21; }

    public MultiPacket(@NonNull ByteBuf data) { read(data); }

    public MultiPacket() {
        this.packets = new ArrayList<>();
    }

    public void read(@NonNull ByteBuf buffer) {
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

            byte[] packetData = new byte[buffer.readInt()]; // Create packet data array with packet length given
            buffer.readBytes(packetData);

            Packet packet;
            try {
                packet = packetClass.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                logger.error("Could not instantiate packet " + packetClass.getSimpleName() + ".", ex);
                continue;
            }
            ByteBuf packetBuf = alloc.buffer(packetData.length, packetData.length);
            try {
                packetBuf.writeBytes(packetData);
                packet.read(packetBuf);
            } finally {
                packetBuf.release();
            }

            packets.add(packet);
        }

        checkReadPacket(buffer);
    }

    public void write(@NonNull ByteBuf buffer) {
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

    public @NonNull List<Packet> getPackets() { return packets; }

    public void setPackets(@NonNull List<Packet> packets) { this.packets = packets; }

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
