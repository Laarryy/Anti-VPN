package me.egg82.antivpn.messaging;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.model.ip.GenericIPManager;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.packets.*;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.PacketUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericMessagingHandler implements MessagingHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<UUID, Boolean> messageCache = Caffeine.newBuilder().expireAfterWrite(2L, TimeUnit.MINUTES).expireAfterAccess(30L, TimeUnit.SECONDS).build(k -> Boolean.FALSE);
    private final Object cacheLock = new Object();

    private final GenericIPManager ipManager;
    private final AbstractPlayerManager playerManager;

    public GenericMessagingHandler(GenericIPManager ipManager, AbstractPlayerManager playerManager) {
        this.ipManager = ipManager;
        this.playerManager = playerManager;
    }

    public void handlePacket(UUID messageId, Packet packet) {
        if (isDuplicate(messageId)) {
            return;
        }

        handleGenericPacket(packet);
    }

    private void handleIp(IPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet for " + packet.getIp() + ".");
        }

        IPModel m = new IPModel();
        m.setIp(packet.getIp());
        m.setType(packet.getType());
        m.setCascade(packet.getCascade());
        m.setConsensus(packet.getConsensus());
        ipManager.getIpCache().put(packet.getIp(), m);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (StorageService service : cachedConfig.getStorage()) {
            IPModel model = new IPModel();
            model.setIp(packet.getIp());
            model.setType(packet.getType());
            model.setCascade(packet.getCascade());
            model.setConsensus(packet.getConsensus());
            service.storeModel(model);
        }

        PacketUtil.queuePacket(packet);
    }

    private void handleDeleteIp(DeleteIPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling deletion packet for " + packet.getIp() + ".");
        }

        ipManager.getIpCache().invalidate(packet.getIp());

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (StorageService service : cachedConfig.getStorage()) {
            IPModel model = new IPModel();
            model.setIp(packet.getIp());
            service.deleteModel(model);
        }

        PacketUtil.queuePacket(packet);
    }

    private void handlePlayer(PlayerPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet for " + packet.getUuid() + ".");
        }

        PlayerModel m = new PlayerModel();
        m.setUuid(packet.getUuid());
        m.setMcleaks(packet.getValue());
        playerManager.getPlayerCache().put(packet.getUuid(), m);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (StorageService service : cachedConfig.getStorage()) {
            PlayerModel model = new PlayerModel();
            model.setUuid(packet.getUuid());
            model.setMcleaks(packet.getValue());
            service.storeModel(model);
        }

        PacketUtil.queuePacket(packet);
    }

    private void handleDeletePlayer(DeletePlayerPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling deletion packet for " + packet.getUuid() + ".");
        }

        playerManager.getPlayerCache().invalidate(packet.getUuid());

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (StorageService service : cachedConfig.getStorage()) {
            PlayerModel model = new PlayerModel();
            model.setUuid(packet.getUuid());
            service.deleteModel(model);
        }

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
        } else if (packet instanceof DeleteIPPacket) {
            handleDeleteIp((DeleteIPPacket) packet);
        } else if (packet instanceof DeletePlayerPacket) {
            handleDeletePlayer((DeletePlayerPacket) packet);
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
