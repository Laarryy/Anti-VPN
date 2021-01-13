package me.egg82.antivpn.messaging;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.MathUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessagingService implements MessagingService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static final DecimalFormat ratioFormat = new DecimalFormat("0.#####");

    protected static final ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;

    protected final String name;
    protected UUID serverId;
    protected String serverIdString;
    protected byte[] serverIdBytes;

    protected MessagingHandler handler;

    protected AbstractMessagingService(@NonNull String name) {
        this.name = name;
    }

    public @NonNull String getName() { return name; }

    private static final double TOLERANCE = 1.1; // Compression ratio tolerance. Determines when compression should happen

    protected final byte[] compressData(ByteBuf data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return new byte[0];
        }

        int uncompressedBytes = data.writerIndex();
        int upperBound = (int) Zstd.compressBound(uncompressedBytes) + 5;

        ByteBuf nd = alloc.directBuffer(uncompressedBytes, uncompressedBytes);
        ByteBuf ndd = alloc.directBuffer(upperBound, upperBound);

        try {
            data.readerIndex(0);
            nd.writeBytes(data);
            ByteBuffer d = nd.nioBuffer();
            d.rewind();

            ByteBuffer dest = ndd.nioBuffer();
            long compressedBytes = Zstd.compressDirectByteBuffer(dest, 5, dest.capacity() - 5, d, 0, uncompressedBytes, 9);
            if (Zstd.isError(compressedBytes)) {
                throw new IOException(new ZstdException(compressedBytes));
            }

            if ((double) uncompressedBytes / (double) (compressedBytes + 4L) < TOLERANCE) {
                byte[] out = new byte[uncompressedBytes + 1];
                out[0] = 0x00;
                d.rewind();
                d.get(out, 1, out.length - 1);

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
        } finally {
            nd.release();
            ndd.release();
        }
    }

    protected final @NonNull ByteBuf decompressData(ByteBuf data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return alloc.buffer(0, 0);
        }

        int compressedBytes = data.writerIndex();
        data.readerIndex(0);

        boolean compressed = data.readByte() != 0x00;
        if (!compressed) {
            ByteBuf retVal = alloc.buffer(data.capacity() - 1, data.capacity() - 1);
            retVal.writeBytes(data);

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Received (no) compression: " + compressedBytes + "/" + retVal.capacity() + " (" + ratioFormat.format((double) retVal.capacity() / (double) compressedBytes) + ")");
            }

            return retVal;
        }

        int uncompressedBytes = data.readInt();
        ByteBuf nd = alloc.directBuffer(compressedBytes, compressedBytes);
        ByteBuf ndd = alloc.directBuffer(uncompressedBytes, uncompressedBytes);

        try {
            data.readerIndex(0);
            nd.writeBytes(data);
            ByteBuffer d = nd.nioBuffer();
            d.rewind();

            ByteBuffer dest = ndd.nioBuffer();
            long decompressedBytes = Zstd.decompressDirectByteBuffer(dest, 0, dest.capacity(), d, 5, compressedBytes - 5);
            if (Zstd.isError(decompressedBytes)) {
                throw new IOException(new ZstdException(decompressedBytes));
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Received compression: " + compressedBytes + "/" + dest.capacity() + " (" + ratioFormat.format((double) dest.capacity() / (double) compressedBytes) + ")");
            }

            dest.rewind();
            ByteBuf retVal = alloc.buffer(uncompressedBytes, uncompressedBytes);
            retVal.writeBytes(dest);

            return retVal;
        } finally {
            nd.release();
            ndd.release();
        }
    }

    private final IntList capacities = new IntArrayList();
    private volatile int capacity = 2 * 1024; // Start at 2kb
    private final ReadWriteLock capacityLock = new ReentrantReadWriteLock();

    protected int getInitialCapacity() {
        capacityLock.readLock().lock();
        int c = capacity;
        capacityLock.readLock().unlock();
        return c;
    }

    protected void addCapacity(int capacity) {
        capacities.add(capacity);
        if (capacities.size() >= 50) {
            capacityLock.writeLock().lock();
            if (capacities.size() >= 50) {
                this.capacity = MathUtil.percentile(capacities, 75.0d);
                capacities.clear();
            }
            capacityLock.writeLock().unlock();
        }
    }

    private void printBytes(@NonNull ByteBuf buffer) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Message --");
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
