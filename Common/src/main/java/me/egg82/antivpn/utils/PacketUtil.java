package me.egg82.antivpn.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.DoubleBuffer;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.services.CollectionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PacketUtil {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(PacketUtil.class));

    private static ExecutorService workPool = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("Anti-VPN_Messaging_%d").build());

    private static AtomicInteger currentIndex = new AtomicInteger(-1);

    private PacketUtil() { }

    public static void setPoolSize(int size) {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        workPool = Executors.newFixedThreadPool(size, new ThreadFactoryBuilder().setNameFormat("Anti-VPN_Messaging_%d").build());
    }

    private static final DoubleBuffer<Packet> packetQueue = new DoubleBuffer<>();
    private static final AtomicBoolean requiresSending = new AtomicBoolean(false);

    public static void queuePackets(@NotNull Collection<@NotNull Packet> packets) {
        packetQueue.getWriteBuffer().addAll(packets);
        requiresSending.set(true);
    }

    public static void queuePackets(@NotNull Packet @NotNull ... packets) {
        for (Packet packet : packets) {
            packetQueue.getWriteBuffer().add(packet);
        }
        requiresSending.set(true);
    }

    public static void queuePacket(@NotNull Packet packet) {
        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void repeatPacket(@NotNull UUID messageId, @NotNull Packet packet, @NotNull String fromService) { sendPacket(messageId, packet, fromService); }

    public static void trySendQueue() {
        if (!requiresSending.compareAndSet(true, false)) {
            return;
        }

        packetQueue.swapBuffers();

        UUID messageId = UUID.randomUUID();
        CollectionProvider.getMessageCache().put(messageId, Boolean.TRUE);

        if (packetQueue.getReadBuffer().size() > 1) {
            MultiPacket multi = new MultiPacket();
            Packet packet;
            while ((packet = packetQueue.getReadBuffer().poll()) != null) {
                if (!multi.getPackets().add(packet) && ConfigUtil.getDebugOrFalse()) {
                    logger.info("Skipping duplicate packet " + packet.getClass().getSimpleName());
                }
            }

            if (!multi.getPackets().isEmpty()) {
                sendPacket(messageId, multi, null);
            }
        } else {
            Packet packet = packetQueue.getReadBuffer().poll();
            if (packet != null) {
                sendPacket(messageId, packet, null);
            }
        }
    }

    private static void sendPacket(@NotNull UUID messageId, @NotNull Packet packet, @Nullable String fromService) {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        if (cachedConfig.getMessagingRedundancy()) {
            for (MessagingService service : cachedConfig.getMessaging()) {
                if (service.getName().equals(fromService)) {
                    continue;
                }

                workPool.execute(() -> {
                    try {
                        logger.debug("Sending " + packet.getClass().getSimpleName() + " through " + service.getName());
                        service.sendPacket(messageId, packet);
                    } catch (IOException | TimeoutException ex) {
                        logger.warn("Could not broadcast packet " + packet.getClass().getSimpleName() + " through " + service.getName(), ex);
                    }
                });
            }
        } else {
            if (fromService != null) {
                return;
            }

            int index = getNextService(cachedConfig);
            int initialIndex = index;
            boolean sent = false;

            do {
                MessagingService service = cachedConfig.getMessaging().get(index);

                try {
                    logger.debug("Sending " + packet.getClass().getSimpleName() + " through " + service.getName());
                    service.sendPacket(messageId, packet);
                    sent = true;
                    break;
                } catch (IOException | TimeoutException ex) {
                    logger.warn("Could not broadcast packet " + packet.getClass().getSimpleName() + " through " + service.getName(), ex);
                    index = getNextService(cachedConfig);
                }
            } while (index != initialIndex); // This will be true if we've run through all of our services and wrapped around to the start again

            if (!sent) {
                logger.error("Could not broadcast packet " + packet.getClass().getSimpleName() + " through any available messaging service.");
            }
        }
    }

    private static int getNextService(CachedConfig cachedConfig) {
        return currentIndex.updateAndGet(v -> {
            if (v >= cachedConfig.getMessaging().size() - 1) {
                return 0;
            }
            return v + 1;
        });
    }
}
