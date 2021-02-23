package me.egg82.antivpn.messaging;

import com.rabbitmq.client.*;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.I18NManager;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.utils.PacketUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import org.jetbrains.annotations.NotNull;

public class RabbitMQMessagingService extends AbstractMessagingService {
    // https://www.rabbitmq.com/api-guide.html#recovery
    private ConnectionFactory factory;
    // "Connection" acts as our pool
    // https://stackoverflow.com/questions/10407760/is-there-a-performance-difference-between-pooling-connections-or-channels-in-rab
    private RecoverableConnection connection;

    private volatile boolean closed = false;
    private final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    private static final String EXCHANGE_NAME = "avpn-data";

    private RabbitMQMessagingService(@NotNull String name, @NotNull I18NManager consoleLocalizationManager) {
        super(name, consoleLocalizationManager);
    }

    public void close() {
        queueLock.writeLock().lock();
        try {
            closed = true;
            try {
                connection.close(8000);
            } catch (IOException ignored) { }
        } finally {
            queueLock.writeLock().unlock();
        }
    }

    public boolean isClosed() { return closed || !connection.isOpen(); }

    public static @NotNull Builder builder(@NotNull String name, @NotNull UUID serverId, @NotNull MessagingHandler handler, @NotNull I18NManager consoleLocalizationManager) { return new Builder(name, serverId, handler, consoleLocalizationManager); }

    public static class Builder {
        private final RabbitMQMessagingService service;
        private final ConnectionFactory config = new ConnectionFactory();

        public Builder(@NotNull String name, @NotNull UUID serverId, @NotNull MessagingHandler handler, @NotNull I18NManager consoleLocalizationManager) {
            service = new RabbitMQMessagingService(name, consoleLocalizationManager);
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

            config.setAutomaticRecoveryEnabled(true);
            config.setTopologyRecoveryEnabled(true);
        }

        public @NotNull Builder url(@NotNull String address, int port, @NotNull String vhost) {
            config.setHost(address);
            config.setPort(port);
            config.setVirtualHost(vhost);
            return this;
        }

        public @NotNull Builder credentials(@NotNull String user, @NotNull String pass) {
            config.setUsername(user);
            config.setPassword(pass);
            return this;
        }

        public @NotNull Builder timeout(int timeout) {
            config.setConnectionTimeout(timeout);
            return this;
        }

        public @NotNull RabbitMQMessagingService build() throws IOException, TimeoutException {
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

                ByteBuf b = alloc.buffer(body.length, body.length);
                ByteBuf data = null;
                try {
                    b.writeBytes(body);
                    data = decompressData(b);

                    byte packetId = data.readByte();
                    Class<Packet> packetClass = PacketUtil.getPacketCache().get(packetId);
                    if (packetClass == null) {
                        logger.warn("Got packet ID that doesn't exist: " + packetId);
                        return;
                    }

                    try {
                        handler.handlePacket(UUID.fromString(properties.getMessageId()), getName(), packetClass.getConstructor(ByteBuf.class).newInstance(data));
                    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                        GELFLogger.exception(logger, ex, consoleLocalizationManager, MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", packetClass.getSimpleName());
                    }
                } finally {
                    b.release();
                    if (data != null) {
                        data.release();
                    }
                }
            }
        };
        channel.addShutdownListener(cause -> {
            try {
                bind();
            } catch (IOException ex) {
                GELFLogger.exception(logger, ex, consoleLocalizationManager, MessageKey.ERROR__MESSAGING__NO_BIND);
            }
        });
        channel.basicConsume(queue, true, consumer);
    }

    public void sendPacket(@NotNull UUID messageId, @NotNull Packet packet) throws IOException, TimeoutException {
        queueLock.readLock().lock();
        try (RecoverableChannel channel = getChannel()) {
            ByteBuf buffer = alloc.buffer(getInitialCapacity());
            try {
                buffer.writeByte(packet.getPacketId());
                packet.write(buffer);
                addCapacity(buffer.writerIndex());

                AMQP.BasicProperties properties = getProperties(DeliveryMode.PERSISTENT, messageId);
                channel.exchangeDeclare(EXCHANGE_NAME, ExchangeType.FANOUT.getType(), true);
                channel.basicPublish(EXCHANGE_NAME, "", properties, compressData(buffer));
            } finally {
                buffer.release();
            }
        } finally {
            queueLock.readLock().unlock();
        }
    }

    private @NotNull AMQP.BasicProperties getProperties(@NotNull DeliveryMode deliveryMode, @NotNull UUID messageId) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("sender", serverIdBytes);

        AMQP.BasicProperties.Builder retVal = new AMQP.BasicProperties.Builder();
        retVal.contentType("application/octet-stream");
        retVal.messageId(messageId.toString());
        retVal.deliveryMode(deliveryMode.getMode());
        retVal.headers(headers);
        return retVal.build();
    }

    private boolean validateProperties(@NotNull AMQP.BasicProperties properties) {
        byte[] data = (byte[]) properties.getHeaders().get("sender");
        ByteBuf buffer = alloc.buffer(16, 16);
        UUID sender;
        try {
            buffer.writeBytes(data);
            sender = new UUID(buffer.readLong(), buffer.readLong());
        } finally {
            buffer.release();
        }
        if (serverId.equals(sender)) {
            return false;
        }
        if (!ValidationUtil.isValidUuid(properties.getMessageId())) {
            logger.warn("Non-valid message ID received: \"" + properties.getMessageId() + "\".");
            return false;
        }
        return true;
    }

    private @NotNull RecoverableConnection getConnection() throws IOException, TimeoutException { return (RecoverableConnection) factory.newConnection(); }

    private @NotNull RecoverableChannel getChannel() throws IOException { return (RecoverableChannel) connection.createChannel(); }

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
        ExchangeType(@NotNull String type) { this.type = type; }
        public @NotNull String getType() { return type; }
    }
}
