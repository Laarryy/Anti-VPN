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

    private static ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-Messaging-%d").build());

    private PacketUtil() { }

    private static final Byte2ObjectMap<Class<Packet>> packetCache = new Byte2ObjectArrayMap<>();

    static {
        List<Class<Packet>> packetClasses = PackageFilter.getClasses(Packet.class, "me.egg82.antivpn.messaging.packets", false, false, false);
        for (Class<Packet> clazz : packetClasses) {
            Packet packet;
            try {
                packet = clazz.newInstance();
            } catch (IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException ex) {
                LoggerFactory.getLogger(AbstractMessagingService.class).error("Could not instantiate packet " + clazz.getSimpleName() + ".", ex);
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

    private static final ConcurrentMap<UUID, FakePlayerModel> playerModels = new ConcurrentHashMap<>();
    private static final DoubleBuffer<Packet> packetQueue = new DoubleBuffer<>();
    private static final AtomicBoolean requiresSending = new AtomicBoolean(false);

    public static void spawnPlayer(Player player) {
        FakePlayerModel model = new FakePlayerModel();
        playerModels.put(player.getUniqueId(), model);

        model.setName(player.getName());
        model.setUuid(player.getUniqueId());
        model.setDisplayName(player.getDisplayName());
        model.setGameMode(player.getGameMode());
        model.setPing(-1); // TODO: Set ping & create method that checks/sets player ping if it changes
        model.setLocation(player.getLocation().clone());
        model.setOnGround(!player.isFlying());
        Location head = player.getEyeLocation();
        model.setHeadPitch(head.getPitch());
        model.setHeadYaw(head.getYaw());

        PlayerSpawnPacket packet = new PlayerSpawnPacket();
        packet.setName(model.getName());
        packet.setUuid(model.getUuid());
        packet.setDisplayName(model.getDisplayName());
        packet.setGameMode(model.getGameMode().ordinal());
        packet.setPing(model.getPing());
        Location body = model.getLocation();
        packet.setWorld(body.getWorld().getName());
        packet.setX(body.getX());
        packet.setY(body.getY());
        packet.setZ(body.getZ());
        packet.setPitch(body.getPitch());
        packet.setYaw(body.getYaw());
        packet.setOnGround(model.isOnGround());
        packet.setHeadPitch(model.getHeadPitch());
        packet.setHeadYaw(model.getHeadYaw());

        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void setEntityLocation(UUID uuid, Location newLocation, boolean isOnGround) {
        FakePlayerModel model = playerModels.get(uuid);
        if (model == null) {
            logger.warn("Got data for model that does not exist: " + uuid);
            return;
        }

        model.setLocation(newLocation);
        model.setOnGround(isOnGround);
    }

    public static void setPlayerPing(UUID uuid, int ping) {
        FakePlayerModel model = playerModels.get(uuid);
        if (model == null) {
            logger.warn("Got data for model that does not exist: " + uuid);
            return;
        }

        model.setPing(ping);
    }

    public static void doEntityAnimation(UUID uuid, int animation) {
        EntityAnimationPacket packet = new EntityAnimationPacket();
        packet.setUuid(uuid);
        packet.setAnimation(animation);

        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void despawnEntity(UUID uuid) {
        if (playerModels.remove(uuid) == null) {
            logger.warn("Got despawn for model that does not exist: " + uuid);
        }

        EntityDespawnPacket packet = new EntityDespawnPacket();
        packet.setUuid(uuid);

        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void queuePacket(Packet packet) {
        packetQueue.getWriteBuffer().add(packet);
        requiresSending.set(true);
    }

    public static void trySendQueue() {
        consolidateModels();
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

    private static void consolidateModels() {
        for (FakePlayerModel model : playerModels.values()) {
            if (model.isLocationChanged()) {
                if (model.isRotationChanged()) {
                    EntityLocationRotationPacket packet = new EntityLocationRotationPacket();
                    packet.setUuid(model.getUuid());
                    Location to = model.getLocation();
                    packet.setWorld(to.getWorld().getName());
                    packet.setX(to.getX());
                    packet.setY(to.getY());
                    packet.setZ(to.getZ());
                    packet.setPitch(to.getPitch());
                    packet.setYaw(to.getYaw());
                    packet.setOnGround(model.isOnGround());
                    packet.setHeadPitch(model.getHeadPitch());
                    packet.setHeadYaw(model.getHeadYaw());

                    packetQueue.getWriteBuffer().add(packet);
                    requiresSending.set(true);
                } else {
                    EntityLocationPacket packet = new EntityLocationPacket();
                    packet.setUuid(model.getUuid());
                    Location to = model.getLocation();
                    packet.setWorld(to.getWorld().getName());
                    packet.setX(to.getX());
                    packet.setY(to.getY());
                    packet.setZ(to.getZ());
                    packet.setOnGround(model.isOnGround());

                    packetQueue.getWriteBuffer().add(packet);
                    requiresSending.set(true);
                }
            } else if (model.isRotationChanged()) {
                EntityRotationPacket packet = new EntityRotationPacket();
                packet.setUuid(model.getUuid());
                Location to = model.getLocation();
                packet.setPitch(to.getPitch());
                packet.setYaw(to.getYaw());
                packet.setHeadPitch(model.getHeadPitch());
                packet.setHeadYaw(model.getHeadYaw());

                packetQueue.getWriteBuffer().add(packet);
                requiresSending.set(true);
            }
        }
    }
}
