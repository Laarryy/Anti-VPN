package me.egg82.antivpn.messaging;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.ValidationUtil;

public class RabbitMQMessagingService extends AbstractMessagingService {
    // https://www.rabbitmq.com/api-guide.html#recovery
    private ConnectionFactory factory;
    // "Connection" acts as our pool
    // https://stackoverflow.com/questions/10407760/is-there-a-performance-difference-between-pooling-connections-or-channels-in-rab
    private RecoverableConnection connection;

    private volatile boolean closed = false;
    private ReadWriteLock queueLock = new ReentrantReadWriteLock();

    private static final String EXCHANGE_NAME = "avpn-data";

    private RabbitMQMessagingService() { }

    public void close() {
        queueLock.writeLock().lock();
        closed = true;
        try {
            connection.close(8000);
        } catch (IOException ignored) { }
        queueLock.writeLock().unlock();
    }

    public boolean isClosed() { return closed || !connection.isOpen(); }

    public static Builder builder(String name, UUID serverId, MessagingHandler handler) { return new Builder(name, serverId, handler); }

    public static class Builder {
        private final RabbitMQMessagingService service = new RabbitMQMessagingService();
        private final ConnectionFactory config = new ConnectionFactory();

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
            try {
                service.handlePacketMethod = handler.getClass().getMethod("handlePacket", UUID.class, Packet.class);
            } catch (NoSuchMethodException | SecurityException ex) {
                service.logger.error("Could not get packet handler method.", ex);
            }

            config.setAutomaticRecoveryEnabled(true);
            config.setTopologyRecoveryEnabled(true);
        }

        public Builder url(String address, int port, String vhost) {
            config.setHost(address);
            config.setPort(port);
            config.setVirtualHost(vhost);
            return this;
        }

        public Builder credentials(String user, String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public Builder timeout(int timeout) {
            config.setConnectionTimeout(timeout);
            return this;
        }

        public RabbitMQMessagingService build() throws IOException, TimeoutException {
            service.factory = config;
            service.connection = service.getConnection();
            service.bind();
            return service;
        }
    }

    private void bind() throws IOException {
        RecoverableChannel channel = getChannel();
        channel.exchangeDeclare(EXCHANGE_NAME,  ExchangeType.FANOUT.getType(), true);
        String queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, EXCHANGE_NAME, "");
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String tag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Got message from exchange: " + envelope.getExchange());
                }
                if (!validateProperties(properties)) {
                    return;
                }

                ByteBuffer b = ByteBuffer.allocateDirect(body.length);
                b.put(body);
                ByteBuffer data = decompressData(b);

                byte packetId = data.get();
                Class<Packet> packetClass = PacketUtil.getPacketCache().get(packetId);
                if (packetClass == null) {
                    logger.warn("Got packet ID that doesn't exist: " + packetId);
                    return;
                }

                Packet packet;
                try {
                    packet = packetClass.newInstance();
                } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                    logger.error("Could not instantiate packet.", ex);
                    return;
                }
                packet.read(data);

                try {
                    handlePacketMethod.invoke(handler, UUID.fromString(properties.getMessageId()), packet);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    logger.error("Could not invoke handler.", ex);
                }
            }
        };
        channel.addShutdownListener(cause -> {
            try {
                bind();
            } catch (IOException ex) {
                logger.error("Could not re-bind channel.", ex);
            }
        });
        channel.basicConsume(queue, true, consumer);
    }

    public void sendPacket(UUID messageId, Packet packet) throws IOException, TimeoutException {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId cannot be null.");
        }
        if (packet == null) {
            throw new IllegalArgumentException("packet cannot be null.");
        }

        queueLock.readLock().lock();
        try (RecoverableChannel channel = getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(8 * 1024); // 8 KB
            buffer.put(packet.getPacketId());
            packet.write(buffer);

            AMQP.BasicProperties properties = getProperties(DeliveryMode.PERSISTENT, messageId);
            channel.exchangeDeclare(EXCHANGE_NAME, ExchangeType.FANOUT.getType(), true);
            channel.basicPublish(EXCHANGE_NAME, "", properties, compressData(buffer));
        } catch (IOException | TimeoutException ex) {
            queueLock.readLock().unlock();
            throw ex;
        }
        queueLock.readLock().unlock();
    }

    private AMQP.BasicProperties getProperties(DeliveryMode deliveryMode, UUID messageId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("sender", serverIdBytes);

        AMQP.BasicProperties.Builder retVal = new AMQP.BasicProperties.Builder();
        retVal.contentType("application/octet-stream");
        retVal.messageId(messageId.toString());
        retVal.deliveryMode(deliveryMode.getMode());
        retVal.headers(headers);
        return retVal.build();
    }

    private boolean validateProperties(AMQP.BasicProperties properties) {
        ByteBuffer buffer = ByteBuffer.wrap((byte[]) properties.getHeaders().get("sender"));
        UUID sender = new UUID(buffer.getLong(), buffer.getLong());
        if (serverId.equals(sender)) {
            return false;
        }
        if (!ValidationUtil.isValidUuid(properties.getMessageId())) {
            logger.warn("Non-valid message ID received: \"" + properties.getMessageId() + "\".");
            return false;
        }
        return true;
    }

    private RecoverableConnection getConnection() throws IOException, TimeoutException { return (RecoverableConnection) factory.newConnection(); }

    private RecoverableChannel getChannel() throws IOException { return (RecoverableChannel) connection.createChannel(); }

    private enum DeliveryMode {
        /**
         * Not logged to disk
         */
        TRANSIENT(1),
        /**
         * When in a durable exchange, logged to disk
         */
        PERSISTENT(2);

        private final int mode;
        DeliveryMode(int mode) { this.mode = mode; }
        public int getMode() { return mode; }
    }

    private enum ExchangeType {
        DIRECT("direct"),
        FANOUT("fanout"),
        TOPIC("topic"),
        HEADERS("match"); // AMQP compatibility

        private final String type;
        ExchangeType(String type) { this.type = type; }
        public String getType() { return type; }
    }
}
