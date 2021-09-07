package me.egg82.antivpn.messaging;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.buffer.ByteBuf;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.KeepAlivePacket;
import me.egg82.antivpn.messaging.packets.server.PacketVersionRequestPacket;
import me.egg82.antivpn.services.CollectionProvider;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RedisMessagingService extends AbstractMessagingService {
    private final ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("Anti-VPN_Redis_%d").build());

    private JedisPool pool;
    private final PubSub pubSub = new PubSub(this);

    private volatile boolean closed = false;
    private final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    private static final String CHANNEL_NAME = "avpn-data";
    private static final byte[] CHANNEL_NAME_BYTES = CHANNEL_NAME.getBytes(StandardCharsets.UTF_8);

    private RedisMessagingService(@NotNull String name, long startupDelay, @NotNull File packetDirectory) {
        super(name, startupDelay, packetDirectory);
    }

    @Override
    public void close() {
        queueLock.writeLock().lock();
        try {
            closed = true;
            workPool.shutdown();
            try {
                if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                    workPool.shutdownNow();
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            pool.close();
        } finally {
            queueLock.writeLock().unlock();
        }
    }

    @Override
    public boolean isClosed() { return closed || pool.isClosed(); }

    public static @NotNull Builder builder(
            @NotNull String name,
            @NotNull UUID serverId,
            @NotNull MessagingHandler handler,
            long startupDelay,
            @NotNull File packetDirectory
    ) { return new Builder(name, serverId, handler, startupDelay, packetDirectory); }

    public static class Builder {
        private final RedisMessagingService service;
        private final JedisPoolConfig config = new JedisPoolConfig();

        private String address = "127.0.0.1";
        private int port = 6379;
        private int timeout = 5000;
        private String pass = "";

        public Builder(@NotNull String name, @NotNull UUID serverId, @NotNull MessagingHandler handler, long startupDelay, @NotNull File packetDirectory) {
            service = new RedisMessagingService(name, startupDelay, packetDirectory);
            service.serverId = serverId;
            service.serverIdString = serverId.toString();
            ByteBuf buffer = alloc.buffer(16, 16);
            try {
                buffer.writeLong(serverId.getMostSignificantBits());
                buffer.writeLong(serverId.getLeastSignificantBits());
                if (buffer.isDirect()) {
                    service.serverIdBytes = new byte[16];
                    buffer.readBytes(service.serverIdBytes);
                } else {
                    service.serverIdBytes = buffer.array();
                }
            } finally {
                buffer.release();
            }

            service.handler = handler;
        }

        public @NotNull Builder url(@NotNull String address, int port) {
            this.address = address;
            this.port = port;
            return this;
        }

        public @NotNull Builder credentials(@NotNull String pass) {
            this.pass = pass;
            return this;
        }

        public @NotNull Builder poolSize(int min, int max) {
            config.setMinIdle(min);
            config.setMaxTotal(max);
            return this;
        }

        public @NotNull Builder life(long lifetime, int timeout) {
            config.setMinEvictableIdleTimeMillis(lifetime);
            config.setMaxWaitMillis(timeout);
            this.timeout = timeout;
            return this;
        }

        public @NotNull RedisMessagingService build() {
            service.pool = new JedisPool(config, address, port, timeout, pass == null || pass.isEmpty() ? null : pass);
            // Warm up pool
            // https://partners-intl.aliyun.com/help/doc-detail/98726.htm
            warmup(service.pool);
            // Indefinite subscription
            subscribe();
            return service;
        }

        private void subscribe() {
            service.workPool.execute(() -> {
                if (service.startupDelay > 0) {
                    try {
                        Thread.sleep(service.startupDelay);
                    } catch (InterruptedException ex) {
                        service.logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        Thread.currentThread().interrupt();
                    }
                }

                while (!service.isClosed()) {
                    try (Jedis redis = service.pool.getResource()) {
                        redis.subscribe(
                                service.pubSub,
                                CHANNEL_NAME_BYTES
                        );
                    } catch (JedisException ex) {
                        if (!service.isClosed()) {
                            service.logger.warn("Redis pub/sub disconnected. Reconnecting..");
                        }
                    }
                }
            });
        }

        private void warmup(@NotNull JedisPool pool) {
            Jedis[] warmpupArr = new Jedis[config.getMinIdle()];

            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                jedis = pool.getResource();
                warmpupArr[i] = jedis;
                jedis.ping();
            }
            // Two loops because we need to ensure we don't pull a freshly-created resource from the pool
            for (int i = 0; i < config.getMinIdle(); i++) {
                Jedis jedis;
                jedis = warmpupArr[i];
                jedis.close();
            }
        }
    }

    private static class PubSub extends BinaryJedisPubSub {
        private final RedisMessagingService service;

        private PubSub(@NotNull RedisMessagingService service) { this.service = service; }

        @Override
        public void onMessage(byte @NotNull [] c, byte @NotNull [] m) {
            String channel = new String(c, StandardCharsets.UTF_8);
            if (ConfigUtil.getDebugOrFalse()) {
                service.logger.debug("Got message from channel: " + channel);
            }

            try {
                if (CHANNEL_NAME.equals(channel)) {
                    handleMessage(m);
                } else {
                    service.logger.warn("Got data from channel that should not exist: " + channel);
                }
            } catch (IOException ex) {
                service.logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_HANDLE), ex);
            }
        }

        private void handleMessage(byte @NotNull [] body) throws IOException {
            ByteBuf b = alloc.buffer(body.length, body.length);
            ByteBuf data = null;
            try {
                b.writeBytes(body);
                data = service.decompressData(b);

                if (ConfigUtil.getHiddenConfig() != null && ConfigUtil.getHiddenConfig().doPacketDump()) {
                    service.dumpReceivedPacket(data);
                }

                UUID sender = new UUID(data.readLong(), data.readLong());
                if (service.serverId.equals(sender)) {
                    return;
                }

                byte packetVersion = CollectionProvider.getServerVersions().getOrDefault(sender, (byte) -1);
                if (packetVersion > -1 && packetVersion != Packet.VERSION) {
                    service.logger.warn("Server " + sender + " packet version " + String.format(
                            "0x%02X ",
                            packetVersion
                    ) + " does not match current packet version " + String.format("0x%02X ", Packet.VERSION) + ". Skipping packet.");
                    return;
                }

                UUID messageId = new UUID(data.readLong(), data.readLong());

                byte packetId = data.readByte();
                Packet packet;
                try {
                    packet = PacketManager.read(packetId, sender, data);
                    if (packet == null) {
                        service.logger.warn("Received packet ID that doesn't exist: " + packetId);
                        return;
                    }
                } catch (Exception ex) {
                    Class<? extends Packet> clazz = PacketManager.getPacket(packetId);
                    service.logger.error(
                            LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", clazz != null ? clazz.getName() : "null"),
                            ex
                    );
                    return;
                }

                if (packetVersion == -1 && packet instanceof KeepAlivePacket) {
                    // Don't send warning
                    return;
                }

                if (packetVersion == -1 && !hasVersion(packet)) {
                    service.logger.warn("Server " + sender + " packet version is unknown, and packet type is of " + packet.getClass().getName() + ". Skipping packet.");
                    // There's a potential race condition here with double-sending a request, but it doesn't really matter
                    ByteBuf finalData = data;
                    CollectionProvider.getPacketProcessingQueue().compute(sender, (k, v) -> {
                        if (v == null) {
                            v = new CopyOnWriteArrayList<>();
                        }

                        if (v.isEmpty()) {
                            if (packet.verifyFullRead(finalData)) {
                                v.add(new Pair<>(messageId, packet));
                            }
                            PacketUtil.queuePacket(new PacketVersionRequestPacket(sender, service.serverId));
                        } else {
                            if (packet.verifyFullRead(finalData)) {
                                v.add(new Pair<>(messageId, packet));
                            }
                        }

                        return v;
                    });
                    return;
                }

                if (packet.verifyFullRead(data)) {
                    service.handler.handlePacket(messageId, service.getName(), packet);
                }
            } finally {
                b.release();
                if (data != null) {
                    data.release();
                }
            }
        }
    }

    @Override
    public void sendPacket(@NotNull UUID messageId, @NotNull Packet packet) throws IOException {
        queueLock.readLock().lock();
        try (Jedis redis = pool.getResource()) {
            ByteBuf buffer = alloc.buffer(getInitialCapacity());
            try {
                buffer.writeBytes(serverIdBytes);
                buffer.writeLong(messageId.getMostSignificantBits());
                buffer.writeLong(messageId.getLeastSignificantBits());
                buffer.writeByte(PacketManager.getId(packet.getClass()));
                packet.write(buffer);
                addCapacity(buffer.writerIndex());

                if (ConfigUtil.getHiddenConfig().doPacketDump()) {
                    dumpSentPacket(buffer);
                }

                redis.publish(CHANNEL_NAME_BYTES, compressData(buffer));
            } finally {
                buffer.release();
            }
        } catch (JedisException ex) {
            throw new IOException(ex);
        } finally {
            queueLock.readLock().unlock();
        }
    }
}
