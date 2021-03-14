package me.egg82.antivpn.messaging.handler;

import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.packets.MultiPacket;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.reflect.PackageFilter;
import me.egg82.antivpn.services.CollectionProvider;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MessagingHandlerImpl extends AbstractMessagingHandler implements MessagingHandler {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(MessagingHandlerImpl.class));

    private static final List<AbstractMessagingHandler> handlers = new ArrayList<>();

    static {
        List<Class<AbstractMessagingHandler>> classes = PackageFilter.getClasses(
                AbstractMessagingHandler.class,
                "me.egg82.antivpn.messaging.handler",
                false,
                false,
                false
        );

        for (Class<AbstractMessagingHandler> clazz : classes) {
            if (clazz.equals(MessagingHandlerImpl.class)) {
                continue;
            }

            try {
                handlers.add(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException ex) {
                logger.error("Could not create new handler instance.", ex);
            }
        }
    }

    @Override
    public void handlePacket(@NotNull UUID messageId, @NotNull String fromService, @NotNull Packet packet) {
        if (CollectionProvider.isDuplicateMessage(messageId)) {
            return;
        }

        try {
            if (!handlePacket(packet)) {
                logger.warn("Did not handle packet: " + packet.getClass().getName());
            }
        } catch (Exception ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
        } finally {
            PacketUtil.repeatPacket(messageId, packet, fromService);
        }
    }

    @Override
    protected boolean handlePacket(@NotNull Packet packet) {
        if (packet instanceof MultiPacket) {
            handleMulti((MultiPacket) packet);
            return true;
        }

        for (AbstractMessagingHandler handler : handlers) {
            if (handler.handlePacket(packet)) {
                return true;
            }
        }

        return false;
    }

    private void handleMulti(@NotNull MultiPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling multi-packet.");
        }

        for (Packet p : packet.getPackets()) {
            if (!handlePacket(p)) {
                logger.warn("Did not handle packet: " + packet.getClass().getName());
            }
        }
    }
}
