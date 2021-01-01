package me.egg82.antivpn.messaging.packets;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPacket implements Packet {
    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    // https://github.com/SpigotMC/BungeeCord/blob/master/protocol/src/main/java/net/md_5/bungee/protocol/DefinedPacket.java
    protected final int getVarInt(@NonNull ByteBuffer input) { return getVarInt(input, 5); }

    protected final int getVarInt(@NonNull ByteBuffer input, int maxBytes) {
        int out = 0;
        int bytes = 0;
        byte in;
        do {
            in = input.get();
            out |= (in & 0x7F) << (bytes++ * 7);
            if (bytes > maxBytes) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((in & 0x80) == 0x80);
        return out;
    }

    protected final void putVarInt(int value, @NonNull ByteBuffer output) {
        int part;
        do {
            part = value & 0x7F;
            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }
            output.put((byte) part);
        } while (value != 0);
    }

    protected final int getVarShort(@NonNull ByteBuffer buf) {
        int low = buf.getShort() & 0xFFFF; // convert to unsigned
        int high = 0;
        if ((low & 0x8000) != 0) {
            low = low & 0x7FFF;
            high = buf.get() & 0xFF; // convert to unsigned
        }
        return ((high & 0xFF) << 15) | low;
    }

    protected final void putVarShort(int toWrite, @NonNull ByteBuffer buf) {
        int low = toWrite & 0x7FFF;
        int high = (toWrite & 0x7F8000) >> 15;
        if (high != 0) {
            low = low | 0x8000;
        }
        buf.putShort((short) low);
        if (high != 0) {
            buf.put((byte) high);
        }
    }

    protected final @NonNull UUID getUUID(@NonNull ByteBuffer input) { return new UUID(input.getLong(), input.getLong()); }

    protected final void putUUID(@NonNull UUID value, @NonNull ByteBuffer output) {
        output.putLong(value.getMostSignificantBits());
        output.putLong(value.getLeastSignificantBits());
    }

    protected final @NonNull String getString(@NonNull ByteBuffer buf) {
        int len = getVarInt(buf);
        if (len > Short.MAX_VALUE) {
            throw new RuntimeException(String.format( "Cannot receive string longer than Short.MAX_VALUE (got %s characters)", len));
        }

        byte[] b = new byte[len];
        buf.get(b);

        return new String(b, StandardCharsets.UTF_8);
    }

    protected final void putString(@NonNull String s, @NonNull ByteBuffer buf) {
        if (s.length() > Short.MAX_VALUE) {
            throw new RuntimeException(String.format( "Cannot send string longer than Short.MAX_VALUE (got %s characters)", s.length()));
        }

        byte[] b = s.getBytes(StandardCharsets.UTF_8);
        putVarInt(b.length, buf);
        buf.put(b);
    }

    protected final boolean getBoolean(@NonNull ByteBuffer buf) { return buf.get() != 0x00; }

    protected final void putBoolean(boolean value, @NonNull ByteBuffer buf) { buf.put(value ? (byte) 0x01 : (byte) 0x00); }

    protected final void checkReadPacket(@NonNull ByteBuffer buffer) {
        if (buffer.hasRemaining()) {
            logger.warn(buffer.remaining() + " bytes remain in the packet ByteBuffer after being parsed.");
            if (ConfigUtil.getDebugOrFalse()) {
                printBytes(buffer);
            }
        }
    }

    protected final boolean checkVersion(@NonNull ByteBuffer buffer) {
        byte packetVersion = buffer.get();
        if (packetVersion != VERSION) {
            logger.warn("Received packet version " + String.format("0x%02X ", packetVersion) + " does not match current packet version " + String.format("0x%02X ", VERSION) + ". Skipping packet.");
            return false;
        }
        return true;
    }

    protected final void printBytes(@NonNull ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Packet --");
        sb.append('\n');
        sb.append("Type: " + getClass().getName());
        sb.append('\n');

        sb.append("Bytes:");
        sb.append('\n');
        int index = buffer.position();
        buffer.position(0);
        while (buffer.hasRemaining()) {
            sb.append(String.format("0x%02X ", buffer.get()));
        }
        sb.append('\n');
        buffer.position(0);
        while (buffer.hasRemaining()) {
            sb.append(String.format("%8s ", Integer.toBinaryString(buffer.get() & 0xFF)).replace(' ', '0') + " ");
        }
        sb.append('\n');
        buffer.position(0);
        while (buffer.hasRemaining()) {
            sb.append(buffer.get() + " ");
        }
        buffer.position(index);

        sb.append('\n');
        sb.append("-- End Packet --");

        logger.info(sb.toString());
    }
}
