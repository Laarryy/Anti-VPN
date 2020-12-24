package me.egg82.antivpn.messaging;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.*;
import me.egg82.antivpn.utils.PacketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericMessagingHandler implements MessagingHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Cache<UUID, Boolean> messageCache = Caffeine.newBuilder().expireAfterWrite(2L, TimeUnit.MINUTES).expireAfterAccess(30L, TimeUnit.SECONDS).build();
    private final Object cacheLock = new Object();

    public GenericMessagingHandler() { }

    public void handlePacket(UUID messageId, Packet packet) {
        if (isDuplicate(messageId)) {
            return;
        }

        handleGenericPacket(packet);
    }

    private void handleIp(IPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling IP for " + packet.getId() + " (" + packet.getIp() + ")");
        }

        // TODO: set IP in storage

        PacketUtil.queuePacket(packet);
    }

    private void handlePlayer(PlayerPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling player for " + packet.getId() + " (" + packet.getUuid() + ")");
        }

        // TODO: set player in storage

        PacketUtil.queuePacket(packet);
    }

    private void handleVpn(VPNPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling VPN data for " + packet.getId() + " (" + (packet.isCascade() ? packet.getCascade() : packet.getConsensus()) + ")");
        }

        // TODO: set VPN data in storage

        PacketUtil.queuePacket(packet);
    }

    private void handleMcLeaks(MCLeaksPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling MCLeaks data for " + packet.getId() + " (" + packet.getValue() + ")");
        }

        // TODO: set MCLeaks data in storage

        PacketUtil.queuePacket(packet);
    }

    private void handleMulti(MultiPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling multi-packet.");
        }

        for (Packet p : packet.getPackets()) {
            handleGenericPacket(p);
        }
    }

    private void handleGenericPacket(Packet packet) {
        if (packet instanceof IPPacket) {
            handleIp((IPPacket) packet);
        } else if (packet instanceof PlayerPacket) {
            handlePlayer((PlayerPacket) packet);
        } else if (packet instanceof VPNPacket) {
            handleVpn((VPNPacket) packet);
        } else if (packet instanceof MCLeaksPacket) {
            handleMcLeaks((MCLeaksPacket) packet);
        } else if (packet instanceof MultiPacket) {
            handleMulti((MultiPacket) packet);
        }
    }

    public void cancel() { }

    private boolean isDuplicate(UUID messageId) {
        if (Boolean.TRUE.equals(messageCache.getIfPresent(messageId))) {
            return true;
        }
        // Double-checked locking
        synchronized (cacheLock) {
            if (Boolean.TRUE.equals(messageCache.getIfPresent(messageId))) {
                return true;
            }
            messageCache.put(messageId, Boolean.TRUE);
        }
        return false;
    }
}
