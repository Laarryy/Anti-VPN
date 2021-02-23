package me.egg82.antivpn.messaging.packets;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPacket implements Packet {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final int readVarInt(@NotNull ByteBuf input) { return readVarInt(input, 5); }

    protected final int readVarInt(@NotNull ByteBuf input, int maxBytes) {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = input.readByte();
            out |= (in & 0x7F) << (bytes++ * 7);
            if (bytes > maxBytes) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((in & 0x80) == 0x80);
        return out;
    }

    protected final void writeVarInt(int value, @NotNull ByteBuf output) {
        int part;
        do {
            part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            output.writeByte((byte) part);
        } while (value != 0);
    }

    protected final int readVarShort(@NotNull ByteBuf buf) {
        int low = buf.readShort() & 0xFFFF; // convert to unsigned
        int high = 0;
        if ((low & 0x8000) != 0) {
            low = low & 0x7FFF;
            high = buf.readByte() & 0xFF; // convert to unsigned
        }
        return ((high & 0xFF) << 15) | low;
    }

    protected final void writeVarShort(int toWrite, @NotNull ByteBuf buf) {
        int low = toWrite & 0x7FFF;
        int high = (toWrite & 0x7F8000) >> 15;
        if (high != 0) {
            low = low | 0x8000;
        }
        buf.writeShort((short) low);
        if (high != 0) {
            buf.writeByte((byte) high);
        }
    }

    protected final @NotNull UUID readUUID(@NotNull ByteBuf input) { return new UUID(input.readLong(), input.readLong()); }

    protected final void writeUUID(@NotNull UUID value, @NotNull ByteBuf output) {
        output.writeLong(value.getMostSignificantBits());
        output.writeLong(value.getLeastSignificantBits());
    }

    protected final @NotNull String readString(@NotNull ByteBuf buf) {
        int len = readVarInt(buf);
        if (len > Short.MAX_VALUE) {
            throw new RuntimeException(String.format( "Cannot receive string longer than Short.MAX_VALUE (got %s characters)", len));
        }

        byte[] b = new byte[len];
        buf.readBytes(b);

        return new String(b, StandardCharsets.UTF_8);
    }

    protected final void writeString(@NotNull String s, @NotNull ByteBuf buf) {
        if (s.length() > Short.MAX_VALUE) {
            throw new RuntimeException(String.format( "Cannot send string longer than Short.MAX_VALUE (got %s characters)", s.length()));
        }

        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(b.length, buf);
        buf.writeBytes(b);
    }

    protected final void checkReadPacket(@NotNull ByteBuf buffer) {
        if (buffer.readableBytes() > 0) {
            logger.warn(buffer.readableBytes() + " bytes remain in the packet ByteBuf after being parsed.");
            if (ConfigUtil.getDebugOrFalse()) {
                printBytes(buffer);
            }
        }
    }

    protected final boolean checkVersion(@NotNull ByteBuf buffer) {
        byte packetVersion = buffer.readByte();
        if (packetVersion != VERSION) {
            logger.warn("Received packet version " + String.format("0x%02X ", packetVersion) + " does not match current packet version " + String.format("0x%02X ", VERSION) + ". Skipping packet.");
            return false;
        }
        return true;
    }

    protected final void printBytes(@NotNull ByteBuf buffer) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Packet --");
        sb.append('\n');
        sb.append("Type: " + getClass().getName());
        sb.append('\n');

        sb.append("Bytes:");
        sb.append('\n');
        int index = buffer.readerIndex();
        buffer.readerIndex(0);
        while (buffer.readableBytes() > 0) {
            sb.append(String.format("0x%02X ", buffer.readByte()));
        }
        sb.append('\n');
        buffer.readerIndex(0);
        while (buffer.readableBytes() > 0) {
            sb.append(String.format("%8s ", Integer.toBinaryString(buffer.readByte() & 0xFF)).replace(' ', '0') + " ");
        }
        sb.append('\n');
        buffer.readerIndex(0);
        while (buffer.readableBytes() > 0) {
            sb.append(buffer.readByte() + " ");
        }
        buffer.readerIndex(index);

        sb.append('\n');
        sb.append("-- End Packet --");

        logger.info(sb.toString());
    }
}
