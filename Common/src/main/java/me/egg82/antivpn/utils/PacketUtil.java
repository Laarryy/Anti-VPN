package me.egg82.antivpn.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectArrayMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.DoubleBuffer;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.handler.MessagingHandlerImpl;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.reflect.PackageFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketUtil {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(PacketUtil.class));

    private static ExecutorService workPool = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder().setNameFormat("Anti-VPN_Messaging_%d").build());

    private PacketUtil() { }

    private static final Byte2ObjectMap<Class<Packet>> packetCache = new Byte2ObjectArrayMap<>();

    static {
        List<Class<Packet>> packetClasses = PackageFilter.getClasses(Packet.class, "me.egg82.antivpn.messaging.packets", false, false, false);
        for (Class<Packet> clazz : packetClasses) {
            Packet packet;
            try {
                packet = clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__MESSAGING__BAD_PACKET, "{name}", clazz.getSimpleName()), ex);
                continue;
            }
            packetCache.put(packet.getPacketId(), clazz);
        }
    }

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

    public static @NotNull Byte2ObjectMap<Class<Packet>> getPacketCache() { return packetCache; }

    private static final DoubleBuffer<Packet> packetQueue = new DoubleBuffer<>();
    private static final AtomicBoolean requiresSending = new AtomicBoolean(false);

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
        MessagingHandlerImpl.messageCache.put(messageId, Boolean.TRUE); // Tight coupling, but it's probably fine

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

        for (MessagingService service : cachedConfig.getMessaging()) {
            if (service.getName().equals(fromService)) {
                continue;
            }

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
