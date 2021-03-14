package me.egg82.antivpn.messaging;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.InitializationPacket;
import me.egg82.antivpn.messaging.packets.server.PacketVersionPacket;
import me.egg82.antivpn.utils.MathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMessagingService implements MessagingService {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    private static final DecimalFormat ratioFormat = new DecimalFormat("0.#####");

    protected static final ByteBufAllocator alloc = PooledByteBufAllocator.DEFAULT;

    protected final String name;
    protected UUID serverId;
    protected String serverIdString;
    protected byte[] serverIdBytes;

    protected MessagingHandler handler;

    protected final File sentPacketDirectory;
    protected final File receivedPacketDirectory;

    protected AbstractMessagingService(@NotNull String name, @NotNull File packetDirectory) {
        this.name = name;
        this.sentPacketDirectory = new File(packetDirectory, "sent");
        this.receivedPacketDirectory = new File(packetDirectory, "received");
    }

    public @NotNull String getName() {
        return name;
    }

    private static final double TOLERANCE = 1.1; // Compression ratio tolerance. Determines when compression should happen

    protected final byte @NotNull [] compressData(@Nullable ByteBuf data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return new byte[0];
        }

        data.readerIndex(0);

        int uncompressedBytes = data.writerIndex();
        int upperBound = (int) Zstd.compressBound(uncompressedBytes) + 5;

        ByteBuf nd = alloc.directBuffer(uncompressedBytes, uncompressedBytes);
        ByteBuf ndd = alloc.directBuffer(upperBound, upperBound);

        try {
            data.readBytes(nd);
            ByteBuffer d = nd.nioBuffer(0, uncompressedBytes);

            ByteBuffer dest = ndd.nioBuffer(0, upperBound);
            long compressedBytes = Zstd.compressDirectByteBuffer(dest, 5, upperBound - 5, d, 0, uncompressedBytes, 9);
            if (Zstd.isError(compressedBytes)) {
                throw new IOException(new ZstdException(compressedBytes));
            }

            if ((double) uncompressedBytes / (double) (compressedBytes + 4L) < TOLERANCE) {
                byte[] out = new byte[uncompressedBytes + 1];
                out[0] = 0x00;
                nd.readBytes(out, 1, uncompressedBytes);

                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Sent (no) compression: " + out.length + "/" + uncompressedBytes + " (" + ratioFormat.format((double) uncompressedBytes / (double) out.length) + ")");
                }

                return out;
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Sent compression: " + (compressedBytes + 5) + "/" + uncompressedBytes + " (" + ratioFormat.format((double) uncompressedBytes / (double) (compressedBytes + 5)) + ")");
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

    protected final @NotNull ByteBuf decompressData(@Nullable ByteBuf data) throws IOException {
        if (data == null || data.capacity() == 0) {
            return alloc.buffer(0, 0);
        }

        int compressedBytes = data.writerIndex();
        data.readerIndex(0);

        boolean compressed = data.readByte() != 0x00;
        if (!compressed) {
            ByteBuf retVal = alloc.buffer(compressedBytes - 1, compressedBytes - 1);
            data.readBytes(retVal);

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Received (no) compression: " + compressedBytes + "/" + (compressedBytes - 1) + " (" + ratioFormat.format((double) (compressedBytes - 1) / (double) compressedBytes) + ")");
            }

            return retVal;
        }

        int uncompressedBytes = data.readInt();
        ByteBuf nd = alloc.directBuffer(compressedBytes - 5, compressedBytes - 5);
        ByteBuf ndd = alloc.directBuffer(uncompressedBytes, uncompressedBytes);

        try {
            data.readBytes(nd);
            ByteBuffer d = nd.nioBuffer(0, compressedBytes - 5);

            ByteBuffer dest = ndd.nioBuffer(0, uncompressedBytes);
            long decompressedBytes = Zstd.decompressDirectByteBuffer(dest, 0, uncompressedBytes, d, 0, compressedBytes - 5);
            if (Zstd.isError(decompressedBytes)) {
                throw new IOException(new ZstdException(decompressedBytes));
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Received compression: " + compressedBytes + "/" + uncompressedBytes + " (" + ratioFormat.format((double) uncompressedBytes / (double) compressedBytes) + ")");
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

    private final int[] capacities = new int[150];
    private final AtomicInteger currentCapacity = new AtomicInteger(0);
    private volatile int capacity = 2 * 1024; // Start at 2kb
    private final ReadWriteLock capacityLock = new ReentrantReadWriteLock();

    protected int getInitialCapacity() {
        capacityLock.readLock().lock();
        try {
            return capacity;
        } finally {
            capacityLock.readLock().unlock();
        }
    }

    protected void addCapacity(int capacity) {
        int current = currentCapacity.getAndIncrement();
        if (current < 150) {
            capacities[current] = capacity;
        } else {
            capacityLock.writeLock().lock();
            try {
                this.capacity = MathUtil.percentile(capacities, 80.0d);
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Set initial capacity to " + ratioFormat.format((double) this.capacity / 1024.0d) + "kb");
                }
                currentCapacity.set(0);
            } finally {
                capacityLock.writeLock().unlock();
            }
        }
    }

    protected final AtomicLong currentSendPacket = new AtomicLong(0L);

    protected final void dumpSentPacket(@NotNull ByteBuf buffer) throws IOException {
        long current = currentSendPacket.getAndIncrement();

        if (sentPacketDirectory.exists() && !sentPacketDirectory.isDirectory()) {
            Files.delete(sentPacketDirectory.toPath());
        }
        if (!sentPacketDirectory.exists()) {
            if (!sentPacketDirectory.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
            }
        }
        if (current == 0L && sentPacketDirectory.exists()) {
            Files.delete(sentPacketDirectory.toPath());
            if (!sentPacketDirectory.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
            }
        }
        File fileOnDisk = new File(sentPacketDirectory, "packet-" + current + ".taco");
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        int cap = Math.min(getInitialCapacity(), 4096);

        int index = buffer.readerIndex();
        try (FileOutputStream outputStream = new FileOutputStream(fileOnDisk)) {
            byte[] buf = new byte[cap];
            while (buffer.readableBytes() > 0) {
                int read = Math.min(buf.length, buffer.readableBytes());
                buffer.readBytes(buf, 0, read);
                outputStream.write(buf, 0, read);
            }
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        buffer.readerIndex(index);
    }

    protected final AtomicLong currentReceivePacket = new AtomicLong(0L);

    protected final void dumpReceivedPacket(@NotNull ByteBuf buffer) throws IOException {
        long current = currentReceivePacket.getAndIncrement();

        if (receivedPacketDirectory.exists() && !receivedPacketDirectory.isDirectory()) {
            Files.delete(receivedPacketDirectory.toPath());
        }
        if (!receivedPacketDirectory.exists()) {
            if (!receivedPacketDirectory.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
            }
        }
        if (current == 0L && receivedPacketDirectory.exists()) {
            Files.delete(receivedPacketDirectory.toPath());
            if (!receivedPacketDirectory.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
            }
        }
        File fileOnDisk = new File(receivedPacketDirectory, "packet-" + current + ".taco");
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        int cap = Math.min(getInitialCapacity(), 4096);

        int index = buffer.readerIndex();
        try (FileOutputStream outputStream = new FileOutputStream(fileOnDisk)) {
            byte[] buf = new byte[cap];
            while (buffer.readableBytes() > 0) {
                int read = Math.min(buf.length, buffer.readableBytes());
                buffer.readBytes(buf, 0, read);
                outputStream.write(buf, 0, read);
            }
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        }
        buffer.readerIndex(index);
    }

    protected static boolean hasVersion(@NotNull Packet packet) {
        if (packet instanceof InitializationPacket || packet instanceof PacketVersionPacket) {
            return true;
        }

        if (packet instanceof MultiPacket) {
            MultiPacket mult = (MultiPacket) packet;
            for (Packet p : mult.getPackets()) {
                if (p instanceof InitializationPacket || p instanceof PacketVersionPacket) {
                    return true;
                }
            }
        }
        return false;
    }

    private void printBytes(@NotNull ByteBuf buffer) {
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
            sb.append(String.format("%8s ", Integer.toBinaryString(buffer.readByte() & 0xFF)).replace(' ', '0')).append(' ');
        }
        sb.append('\n');
        buffer.readerIndex(0);
        while (buffer.readableBytes() > 0) {
            sb.append(buffer.readByte()).append(' ');
        }
        buffer.readerIndex(index);

        sb.append('\n');
        sb.append("-- End Message --");

        logger.info(sb.toString());
    }

    private void printBytes(byte @NotNull [] bytes) {
        StringBuilder sb = new StringBuilder();

        sb.append('\n');
        sb.append("-- Begin Message --");
        sb.append('\n');

        sb.append("Bytes:");
        sb.append('\n');
        for (byte b : bytes) {
            sb.append(String.format("0x%02X ", b));
        }
        sb.append('\n');
        for (byte b : bytes) {
            sb.append(String.format("%8s ", Integer.toBinaryString(b & 0xFF)).replace(' ', '0')).append(' ');
        }
        sb.append('\n');
        for (byte b : bytes) {
            sb.append(b).append(' ');
        }

        sb.append('\n');
        sb.append("-- End Message --");

        logger.info(sb.toString());
    }
}
