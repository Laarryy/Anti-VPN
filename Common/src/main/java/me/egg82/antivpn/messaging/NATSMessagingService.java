package me.egg82.antivpn.messaging;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.netty.buffer.ByteBuf;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.messaging.handler.MessagingHandler;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.services.CollectionProvider;
import org.jetbrains.annotations.NotNull;

public class NATSMessagingService extends AbstractMessagingService {
    private Connection connection;
    private Dispatcher dispatcher;

    private volatile boolean closed = false;
    private final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    private static final String SUBJECT_NAME = "avpn-data";

    private NATSMessagingService(@NotNull String name, @NotNull File packetDirectory) {
        super(name, packetDirectory);
    }

    public void close() {
        queueLock.writeLock().lock();
        try {
            closed = true;
            try {
                connection.closeDispatcher(dispatcher);
                connection.close();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        } finally {
            queueLock.writeLock().unlock();
        }
    }

    public boolean isClosed() { return closed || connection.getConnectedUrl() == null; }

    public static @NotNull Builder builder(@NotNull String name, @NotNull UUID serverId, @NotNull MessagingHandler handler, @NotNull File packetDirectory) { return new Builder(name, serverId, handler, packetDirectory); }

    public static class Builder {
        private final NATSMessagingService service;
        private final Options.Builder config = new Options.Builder();

        public Builder(@NotNull String name, @NotNull UUID serverId, @NotNull MessagingHandler handler, @NotNull File packetDirectory) {
            service = new NATSMessagingService(name, packetDirectory);
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
            config.server("nats://" + address + ":" + port);
            return this;
        }

        public @NotNull Builder credentials(@NotNull String file) {
            config.authHandler(Nats.credentials(file));
            return this;
        }

        public @NotNull Builder life(int timeout) {
            config.connectionTimeout(Duration.ofMillis(timeout));
            return this;
        }

        public @NotNull NATSMessagingService build() throws IOException, InterruptedException {
            service.connection = Nats.connect(config.build());
            // Indefinite subscription
            subscribe();
            return service;
        }

        private void subscribe() {
            service.dispatcher = service.connection.createDispatcher(message -> {
                String subject = message.getSubject();
                if (ConfigUtil.getDebugOrFalse()) {
                    service.logger.info("Got message from subject: " + subject);
                }

                try {
                    switch (subject) {
                        case SUBJECT_NAME:
                            handleMessage(message.getData());
                            break;
                        default:
                            service.logger.warn("Got data from subject that should not exist: " + subject);
                            break;
                    }
                } catch (IOException ex) {
                    service.logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_HANDLE), ex);
                }
            });
            service.dispatcher.subscribe(SUBJECT_NAME);
        }

        private void handleMessage(byte @NotNull [] body) throws IOException {
            ByteBuf b = alloc.buffer(body.length, body.length);
            ByteBuf data = null;
            try {
                b.writeBytes(body);
                data = service.decompressData(b);

                if (ConfigUtil.getHiddenConfig().doPacketDump()) {
                    service.dumpReceivedPacket(data);
                }

                UUID sender = new UUID(data.readLong(), data.readLong());
                if (service.serverId.equals(sender)) {
                    return;
                }

                byte packetVersion = CollectionProvider.getServerVersions().getOrDefault(sender, (byte) -1);
                if (packetVersion > -1 && packetVersion != Packet.VERSION) {
                    service.logger.warn("Server " + sender + " packet version " + String.format("0x%02X ", packetVersion) + " does not match current packet version " + String.format("0x%02X ", Packet.VERSION) + ". Skipping packet.");
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
                    service.logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", clazz != null ? clazz.getName() : "null"), ex);
                    return;
                }

                if (packetVersion == -1 && !hasVersion(packet)) {
                    service.logger.warn("Server " + sender + " packet version is unknown, and packet type is of " + packet.getClass().getName() + ". Skipping packet.");
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

    public void sendPacket(@NotNull UUID messageId, @NotNull Packet packet) throws IOException {
        queueLock.readLock().lock();
        try {
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

                connection.publish(SUBJECT_NAME, compressData(buffer));
            } finally {
                buffer.release();
            }
        } catch (IllegalStateException ex) {
            throw new IOException(ex);
        } finally {
            queueLock.readLock().unlock();
        }
    }
}
