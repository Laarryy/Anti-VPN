package me.egg82.antivpn.messaging.handler;

import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.InitializationPacket;
import me.egg82.antivpn.messaging.packets.server.PacketVersionPacket;
import me.egg82.antivpn.messaging.packets.server.ShutdownPacket;
import me.egg82.antivpn.services.CollectionProvider;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;

public class ServerMessagingHandler extends AbstractMessagingHandler {
    protected boolean handlePacket(@NotNull Packet packet) {
        if (packet instanceof InitializationPacket) {
            handleInitialization((InitializationPacket) packet);
            return true;
        } else if (packet instanceof PacketVersionPacket) {
            handlePacketVersion((PacketVersionPacket) packet);
            return true;
        } else if (packet instanceof ShutdownPacket) {
            handleShutdown((ShutdownPacket) packet);
            return true;
        }

        return false;
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
    }

    private void handleShutdown(@NotNull ShutdownPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling shutdown for server " + packet.getServer());
        }

        handleShutdown(packet.getServer());
    }

    private void handleShutdown(@NotNull UUID serverId) {
        CollectionProvider.getServerVersions().removeByte(serverId);
    }
}
