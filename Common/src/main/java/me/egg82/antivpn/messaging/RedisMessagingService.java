package me.egg82.antivpn.messaging;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.utils.PacketUtil;
import redis.clients.jedis.BinaryJedisPubSub;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public class RedisMessagingService extends AbstractMessagingService {
    private ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-Redis-%d").build());

    private JedisPool pool;
    private PubSub pubSub = new PubSub(this);

    private volatile boolean closed = false;
    private ReadWriteLock queueLock = new ReentrantReadWriteLock();

    private static final String CHANNEL_NAME = "avpn-data";
    private static final byte[] CHANNEL_NAME_BYTES = CHANNEL_NAME.getBytes(StandardCharsets.UTF_8);

    private RedisMessagingService() { }

    public void close() {
        queueLock.writeLock().lock();
        closed = true;
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(8L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        pool.close();
        queueLock.writeLock().unlock();
    }

    public boolean isClosed() { return closed || pool.isClosed(); }

    public static Builder builder(String name, UUID serverId, MessagingHandler handler) { return new Builder(name, serverId, handler); }

    public static class Builder {
        private final RedisMessagingService service = new RedisMessagingService();
        private final JedisPoolConfig config = new JedisPoolConfig();

        private String address = "127.0.0.1";
        private int port = 6379;
        private int timeout = 5000;
        private String pass = "";

        public Builder(String name, UUID serverId, MessagingHandler handler) {
            if (name == null) {
                throw new IllegalArgumentException("name cannot be null.");
            }
            if (serverId == null) {
                throw new IllegalArgumentException("serverId cannot be null.");
            }
            if (handler == null) {
                throw new IllegalArgumentException("handler cannot be null.");
            }

            service.name = name;
            service.serverId = serverId;
            service.serverIdString = serverId.toString();
            byte[] bytes = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            buffer.putLong(serverId.getMostSignificantBits());
            service.serverIdBytes = bytes;
            buffer.putLong(serverId.getLeastSignificantBits());

            service.handler = handler;
        }

        public Builder url(String address, int port) {
            this.address = address;
            this.port = port;
            return this;
        }

        public Builder credentials(String pass) {
            this.pass = pass;
            return this;
        }

        public Builder poolSize(int min, int max) {
            config.setMinIdle(min);
            config.setMaxTotal(max);
            return this;
        }

        public Builder life(long lifetime, int timeout) {
            config.setMinEvictableIdleTimeMillis(lifetime);
            config.setMaxWaitMillis(timeout);
            this.timeout = timeout;
            return this;
        }

        public RedisMessagingService build() {
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
                while (!service.isClosed()) {
                    try (Jedis redis = service.pool.getResource()) {
                        redis.subscribe(service.pubSub,
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

        private void warmup(JedisPool pool) {
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

        private PubSub(RedisMessagingService service) { this.service = service; }

        public void onMessage(byte[] c, byte[] m) {
            String channel = new String(c, StandardCharsets.UTF_8);
            if (ConfigUtil.getDebugOrFalse()) {
                service.logger.info("Got message from channel: " + channel);
            }

            try {
                switch (channel) {
                    case CHANNEL_NAME:
                        handleMessage(m);
                        break;
                    default:
                        service.logger.warn("Got data from channel that should not exist: " + channel);
                        break;
                }
            } catch (IOException ex) {
                service.logger.error("Could not handle message.", ex);
            }
        }

        private void handleMessage(byte[] body) throws IOException {
            ByteBuffer b = ByteBuffer.allocateDirect(body.length);
            b.put(body);
            ByteBuffer data = service.decompressData(b);

            UUID sender = new UUID(data.getLong(), data.getLong());
            if (service.serverId.equals(sender)) {
                return;
            }
            UUID messageId = new UUID(data.getLong(), data.getLong());

            byte packetId = data.get();
            Class<Packet> packetClass = PacketUtil.getPacketCache().get(packetId);
            if (packetClass == null) {
                service.logger.warn("Got packet ID that doesn't exist: " + packetId);
                return;
            }

            Packet packet;
            try {
                packet = packetClass.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                service.logger.error("Could not instantiate packet.", ex);
                return;
            }
            packet.read(data);

            service.handler.handlePacket(messageId, packet);
        }
    }

    public void sendPacket(UUID messageId, Packet packet) throws IOException {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId cannot be null.");
        }
        if (packet == null) {
            throw new IllegalArgumentException("packet cannot be null.");
        }

        queueLock.readLock().lock();
        try (Jedis redis = pool.getResource()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024); // 8 KB
            buffer.put(serverIdBytes);
            buffer.putLong(messageId.getMostSignificantBits());
            buffer.putLong(messageId.getLeastSignificantBits());
            buffer.put(packet.getPacketId());
            packet.write(buffer);

            redis.publish(CHANNEL_NAME_BYTES, compressData(buffer));
        } catch (JedisException ex) {
            queueLock.readLock().unlock();
            throw new IOException(ex);
        }
        queueLock.readLock().unlock();
    }
}
