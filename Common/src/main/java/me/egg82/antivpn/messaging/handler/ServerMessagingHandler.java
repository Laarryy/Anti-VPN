package me.egg82.antivpn.messaging.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.*;
import me.egg82.antivpn.services.CollectionProvider;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ServerMessagingHandler extends AbstractMessagingHandler {
    private final Cache<UUID, Long> aliveServers = Caffeine.newBuilder().expireAfterWrite(20L, TimeUnit.SECONDS)
            .evictionListener((RemovalListener<UUID, Long>) (uuid, timestamp, cause) -> {
                logger.warn("Server " + uuid + " has either shut down or timed out. Clearing its data.");
                if (uuid != null) {
                    handleShutdown(uuid);
                }
            }).build();

    @Override
    protected boolean handlePacket(@NotNull Packet packet) {
        if (packet instanceof KeepAlivePacket) {
            handleKeepalive((KeepAlivePacket) packet);
            return true;
        } else if (packet instanceof InitializationPacket) {
            handleInitialization((InitializationPacket) packet);
            return true;
        } else if (packet instanceof PacketVersionPacket) {
            handlePacketVersion((PacketVersionPacket) packet);
            return true;
        } else if (packet instanceof PacketVersionRequestPacket) {
            handlePacketVersionRequest((PacketVersionRequestPacket) packet);
            return true;
        } else if (packet instanceof ShutdownPacket) {
            handleShutdown((ShutdownPacket) packet);
            return true;
        }

        return false;
    }

    private void handleKeepalive(@NotNull KeepAlivePacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.debug("Handling keep alive for " + packet.getSender());
        }

        aliveServers.put(packet.getSender(), System.currentTimeMillis());
    }

    private void handleInitialization(@NotNull InitializationPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling initialization for server " + packet.getServer());
        }

        CollectionProvider.getServerVersions().put(packet.getServer(), packet.getPacketVersion());
        PacketUtil.queuePacket(new PacketVersionPacket(packet.getServer(), ConfigUtil.getCachedConfig().getServerId(), Packet.VERSION));
    }

    private void handlePacketVersion(@NotNull PacketVersionPacket packet) {
        if (!packet.getIntendedRecipient().equals(ConfigUtil.getCachedConfig().getServerId())) {
            return;
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet version for server " + packet.getServer());
        }

        CollectionProvider.getServerVersions().put(packet.getServer(), packet.getPacketVersion());

        MessagingService firstService = ConfigUtil.getCachedConfig().getMessaging().get(0);
        firstService.flushPacketQueue(packet.getServer());
    }

    private void handlePacketVersionRequest(@NotNull PacketVersionRequestPacket packet) {
        if (!packet.getIntendedRecipient().equals(ConfigUtil.getCachedConfig().getServerId())) {
            return;
        }

        if (ConfigUtil.getDebugOrFalse()) {
            logger.debug("Handling packet version request from server " + packet.getServer());
        }

        PacketUtil.queuePacket(new PacketVersionPacket(packet.getServer(), packet.getIntendedRecipient(), Packet.VERSION));
    }

    private void handleShutdown(@NotNull ShutdownPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling shutdown for server " + packet.getServer());
        }

        aliveServers.invalidate(packet.getServer());
        handleShutdown(packet.getServer());
    }

    private void handleShutdown(@NotNull UUID serverId) {
        CollectionProvider.getServerVersions().removeByte(serverId);
    }
}
