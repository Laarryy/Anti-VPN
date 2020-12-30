package me.egg82.antivpn.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.DoubleBuffer;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import ninja.egg82.reflect.PackageFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketUtil {
    private static final Logger logger = LoggerFactory.getLogger(PacketUtil.class);

    private static ExecutorService workPool = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("AntiVPN-Messaging-%d").build());

    private PacketUtil() { }

    private static final Byte2ObjectMap<Class<Packet>> packetCache = new Byte2ObjectArrayMap<>();

    static {
        List<Class<Packet>> packetClasses = PackageFilter.getClasses(Packet.class, "me.egg82.antivpn.messaging.packets", false, false, false);
        for (Class<Packet> clazz : packetClasses) {
            Packet packet;
            try {
                packet = clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                logger.error("Could not instantiate packet " + clazz.getSimpleName() + ".", ex);
                continue;
            }
            packetCache.put(packet.getPacketId(), clazz);
        }
    }

    public static void setPoolSize(int size) {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(8L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        workPool = Executors.newFixedThreadPool(size, new ThreadFactoryBuilder().setNameFormat("AntiVPN-Messaging-%d").build());
    }

    public static Byte2ObjectMap<Class<Packet>> getPacketCache() { return packetCache; }

    private static final DoubleBuffer<Packet> packetQueue = new DoubleBuffer<>();
    private static final AtomicBoolean requiresSending = new AtomicBoolean(false);

    public static void queuePacket(Packet packet) {
        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void trySendQueue() {
        if (!requiresSending.compareAndSet(true, false)) {
            return;
        }

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Could not get cached config.");
            return;
        }

        packetQueue.swapBuffers();

        UUID messageId = UUID.randomUUID();

        // TODO: avoid sending packets back on the received messaging service
        if (packetQueue.getReadBuffer().size() > 1) {
            MultiPacket multi = new MultiPacket();
            Packet packet;
            while ((packet = packetQueue.getReadBuffer().poll()) != null) {
                multi.getPackets().add(packet);
            }

            // TODO: Split workload across several messaging services?
            for (MessagingService service : cachedConfig.getMessaging()) {
                workPool.execute(() -> {
                    try {
                        service.sendPacket(messageId, multi);
                    } catch (IOException | TimeoutException ex) {
                        logger.warn("Could not broadcast packet " + multi.getClass().getSimpleName() + " through " + service.getName(), ex);
                    }
                });
            }
        } else {
            Packet packet = packetQueue.getReadBuffer().poll();

            // TODO: Split workload across several messaging services?
            for (MessagingService service : cachedConfig.getMessaging()) {
                workPool.execute(() -> {
                    try {
                        service.sendPacket(messageId, packet);
                    } catch (IOException | TimeoutException ex) {
                        logger.warn("Could not broadcast packet " + packet.getClass().getSimpleName() + " through " + service.getName(), ex);
                    }
                });
            }
        }
    }
}
