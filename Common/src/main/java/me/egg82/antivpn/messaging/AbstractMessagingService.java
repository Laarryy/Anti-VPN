package me.egg82.antivpn.messaging;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessagingService implements MessagingService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final DecimalFormat ratioFormat = new DecimalFormat("0.#####");

    protected String name;
    protected UUID serverId;
    protected String serverIdString;
    protected byte[] serverIdBytes;

    protected MessagingHandler handler;

    protected Method handlePacketMethod;

    public String getName() { return name; }

    private static final double TOLERANCE = 1.1; // Compression ratio tolerance. Determines when compression should happen

    protected final byte[] compressData(ByteBuffer data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return new byte[0];
        }

        int uncompressedBytes = data.position();
        data.rewind();

        ByteBuffer dest = ByteBuffer.allocateDirect((int) Zstd.compressBound(uncompressedBytes) + 5);
        long compressedBytes = Zstd.compressDirectByteBuffer(dest, 5, dest.capacity() - 5, data, 0, uncompressedBytes, 9);
        if (Zstd.isError(compressedBytes)) {
            throw new IOException(new ZstdException(compressedBytes));
        }

        if ((double) uncompressedBytes / (double) (compressedBytes + 4L) < TOLERANCE) {
            byte[] out = new byte[uncompressedBytes + 1];
            out[0] = 0x00;
            data.rewind();
            data.get(out, 1, out.length - 1);

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Sent (no) compression: " + out.length + "/" + (out.length - 1) + " (" + ratioFormat.format((double) (out.length - 1) / (double) out.length) + ")");
            }

            return out;
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Sent compression: " + compressedBytes + "/" + uncompressedBytes + " (" + ratioFormat.format((double) uncompressedBytes / (double) compressedBytes) + ")");
        }

        dest.put(0, (byte) 0x01);
        dest.putInt(1, uncompressedBytes);
        dest.rewind();

        byte[] out = new byte[(int) compressedBytes + 5];
        dest.get(out);
        return out;
    }

    protected final ByteBuffer decompressData(ByteBuffer data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return ByteBuffer.allocateDirect(0);
        }

        int compressedBytes = data.position();
        data.rewind();

        boolean compressed = data.get() != 0x00;
        if (!compressed) {
            ByteBuffer retVal = ByteBuffer.allocateDirect(data.capacity() - 1);
            retVal.put(data);

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Received (no) compression: " + compressedBytes + "/" + retVal.capacity() + " (" + ratioFormat.format((double) retVal.capacity() / (double) compressedBytes) + ")");
            }

            retVal.rewind();
            return retVal;
        }

        ByteBuffer dest = ByteBuffer.allocateDirect(data.getInt());
        long decompressedBytes = Zstd.decompressDirectByteBuffer(dest, 0, dest.capacity(), data, 5, compressedBytes - 5);
        if (Zstd.isError(decompressedBytes)) {
            throw new IOException(new ZstdException(decompressedBytes));
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Received compression: " + compressedBytes + "/" + dest.capacity() + " (" + ratioFormat.format((double) dest.capacity() / (double) compressedBytes) + ")");
        }

        dest.rewind();
        return dest;
    }

    private void printBytes(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Message --");
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
        sb.append("-- End Message --");

        logger.info(sb.toString());
    }

    private void printBytes(byte[] bytes) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Message --");
        sb.append('\n');

        sb.append("Bytes:");
        sb.append('\n');
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("0x%02X ", bytes[i]));
        }
        sb.append('\n');
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%8s ", Integer.toBinaryString(bytes[i] & 0xFF)).replace(' ', '0') + " ");
        }
        sb.append('\n');
        for (int i = 0; i < bytes.length; i++) {
            sb.append(bytes[i] + " ");
        }

        sb.append('\n');
        sb.append("-- End Message --");

        logger.info(sb.toString());
    }
}
