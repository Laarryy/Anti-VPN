package me.egg82.antivpn.messaging.handler;

import java.util.UUID;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.server.InitializationPacket;
import me.egg82.antivpn.messaging.packets.server.ShutdownPacket;
import me.egg82.antivpn.services.CollectionProvider;
import org.jetbrains.annotations.NotNull;

public class ServerMessagingHandler extends AbstractMessagingHandler {
    protected boolean handlePacket(@NotNull Packet packet) {
        if (packet instanceof InitializationPacket) {
            handleInitialization((InitializationPacket) packet);
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
