package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import me.egg82.antivpn.utils.PacketUtil;

public class MultiPacket extends AbstractPacket {
    private List<Packet> packets;

    public byte getPacketId() { return 0x21; }

    public MultiPacket(ByteBuffer data) { read(data); }

    public MultiPacket() {
        this.packets = new ArrayList<>();
    }

    public void read(ByteBuffer buffer) {
        if (!checkVersion(buffer)) {
            return;
        }

        if (this.packets == null) {
            this.packets = new ArrayList<>();
        } else {
            this.packets.clear();
        }

        byte nextPacket;
        while (buffer.remaining() > 0 && (nextPacket = buffer.get()) != 0x00) { // Seek end of multi-packet or end of buffer
            Class<Packet> packetClass = PacketUtil.getPacketCache().get(nextPacket);
            if (packetClass == null) {
                logger.warn("Got packet ID that doesn't exist: " + nextPacket);
                continue;
            }

            byte[] packetData = new byte[buffer.getInt()]; // Create packet data array with packet length given
            buffer.get(packetData);

            Packet packet;
            try {
                packet = packetClass.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                logger.error("Could not instantiate packet " + packetClass.getSimpleName() + ".", ex);
                continue;
            }
            packet.read(ByteBuffer.wrap(packetData));

            packets.add(packet);
        }

        checkReadPacket(buffer);
    }

    public void write(ByteBuffer buffer) {
        buffer.put(VERSION);

        if (packets == null || packets.isEmpty()) {
            buffer.put((byte) 0x00); // End of multi-packet
            return;
        }

        for (Packet packet : packets) {
            if (packet == null) {
                continue;
            }

            buffer.put(packet.getPacketId()); // Write packet ID
            int start = buffer.position();
            buffer.position(start + 4); // Make room for an int at the head
            packet.write(buffer);
            buffer.putInt(start, buffer.position() - start - 4); // Write the packet length to the int at the head
        }

        buffer.put((byte) 0x00); // End of multi-packet
    }

    public List<Packet> getPackets() { return packets; }

    public void setPackets(List<Packet> packets) { this.packets = packets; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MultiPacket)) return false;
        MultiPacket that = (MultiPacket) o;
        return Objects.equals(packets, that.packets);
    }

    public int hashCode() { return Objects.hash(packets); }

    public String toString() {
        return "MultiPacket{" +
                "packets=" + packets +
                '}';
    }
}
