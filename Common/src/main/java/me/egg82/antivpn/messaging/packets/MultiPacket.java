package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.messaging.PacketManager;
import me.egg82.antivpn.utils.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MultiPacket extends AbstractPacket {
    private static final ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;

    private Set<Packet> packets = new LinkedHashSet<>();

    public MultiPacket(@NotNull UUID sender, @NotNull ByteBuf data) {
        super(sender);
        read(data);
    }

    public MultiPacket() {
        super(UUIDUtil.EMPTY_UUID);
    }

    @Override
    public void read(@NotNull ByteBuf buffer) {
        this.packets.clear();

        byte nextPacket;
        while (buffer.readableBytes() > 0 && (nextPacket = buffer.readByte()) != 0x00) { // Seek end of multi-packet or end of buffer
            int packetLen = buffer.readInt();
            ByteBuf packetBuf = alloc.buffer(packetLen, packetLen);
            try {
                buffer.readBytes(packetBuf);

                Packet packet;
                try {
                    packet = PacketManager.read(nextPacket, sender, packetBuf);
                    if (packet == null) {
                        logger.warn("Received packet ID that doesn't exist: " + nextPacket);
                    } else {
                        if (packet.verifyFullRead(packetBuf)) {
                            packets.add(packet);
                        }
                    }
                } catch (Exception ex) {
                    Class<? extends Packet> clazz = PacketManager.getPacket(nextPacket);
                    logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", clazz != null ? clazz.getName() : "null"), ex);
                }
            } finally {
                packetBuf.release();
            }
        }
    }

    @Override
    public void write(@NotNull ByteBuf buffer) {
        if (packets.isEmpty()) {
            buffer.writeByte((byte) 0x00); // End of multi-packet
            return;
        }

        for (Packet packet : packets) {
            if (packet == null) {
                continue;
            }

            buffer.writeByte(PacketManager.getId(packet.getClass())); // Write packet ID
            int start = buffer.writerIndex();
            buffer.writeInt(0); // Make room for an int at the head
            packet.write(buffer);
            buffer.setInt(start, buffer.writerIndex() - start - 4); // Write the packet length to the int at the head
        }

        buffer.writeByte((byte) 0x00); // End of multi-packet
    }

    public @NotNull Set<@NotNull Packet> getPackets() { return packets; }

    public void setPackets(@NotNull Set<@NotNull Packet> packets) {
        this.packets = packets;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultiPacket)) {
            return false;
        }
        MultiPacket that = (MultiPacket) o;
        return packets.equals(that.packets);
    }

    public int hashCode() { return Objects.hash(packets); }

    public String toString() {
        return "MultiPacket{" +
                "sender=" + sender +
                ", packets=" + packets +
                '}';
    }
}
