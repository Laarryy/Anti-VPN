package me.egg82.antivpn.messaging;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.APIUtil;
import me.egg82.antivpn.api.model.ip.AbstractIPManager;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.messaging.packets.*;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.utils.PacketUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericMessagingHandler implements MessagingHandler {
    private final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    public static final LoadingCache<UUID, Boolean> messageCache = Caffeine.newBuilder().expireAfterWrite(2L, TimeUnit.MINUTES).expireAfterAccess(30L, TimeUnit.SECONDS).build(k -> Boolean.FALSE);
    private final Object messageCacheLock = new Object();

    public GenericMessagingHandler() { }

    public void handlePacket(@NotNull UUID messageId, @NotNull String fromService, @NotNull Packet packet) {
        if (isDuplicate(messageId)) {
            return;
        }

        try {
            handleGenericPacket(packet);
        } finally {
            PacketUtil.repeatPacket(messageId, packet, fromService);
        }
    }

    private void handleIp(@NotNull IPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet for " + packet.getIp() + ".");
        }

        AbstractIPManager ipManager = APIUtil.getIpManager();
        if (ipManager == null) {
            logger.error("IP manager could not be fetched.");
            return;
        }

        IPModel m = new IPModel();
        m.setIp(packet.getIp());
        m.setType(packet.getType());
        m.setCascade(packet.getCascade());
        m.setConsensus(packet.getConsensus());
        ipManager.getIpCache().put(new Pair<>(packet.getIp(), AlgorithmMethod.values()[packet.getType()]), m);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        for (StorageService service : cachedConfig.getStorage()) {
            IPModel model = service.getOrCreateIpModel(packet.getIp(), packet.getType());
            model.setCascade(packet.getCascade());
            model.setConsensus(packet.getConsensus());
            service.storeModel(model);
        }
    }

    private void handleDeleteIp(@NotNull DeleteIPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling deletion packet for " + packet.getIp() + ".");
        }

        AbstractIPManager ipManager = APIUtil.getIpManager();
        if (ipManager == null) {
            logger.error("IP manager could not be fetched.");
            return;
        }

        ipManager.getIpCache().invalidate(new Pair<>(packet.getIp(), AlgorithmMethod.CASCADE));
        ipManager.getIpCache().invalidate(new Pair<>(packet.getIp(), AlgorithmMethod.CONSESNSUS));

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        for (StorageService service : cachedConfig.getStorage()) {
            IPModel model = new IPModel();
            model.setIp(packet.getIp());
            service.deleteModel(model);
        }
    }

    private void handlePlayer(@NotNull PlayerPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet for " + packet.getUuid() + ".");
        }

        AbstractPlayerManager playerManager = APIUtil.getPlayerManager();
        if (playerManager == null) {
            logger.error("Player manager could not be fetched.");
            return;
        }

        PlayerModel m = new PlayerModel();
        m.setUuid(packet.getUuid());
        m.setMcleaks(packet.getValue());
        playerManager.getPlayerCache().put(packet.getUuid(), m);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        for (StorageService service : cachedConfig.getStorage()) {
            PlayerModel model = service.getOrCreatePlayerModel(packet.getUuid(), packet.getValue());
            service.storeModel(model);
        }
    }

    private void handleDeletePlayer(@NotNull DeletePlayerPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling deletion packet for " + packet.getUuid() + ".");
        }

        AbstractPlayerManager playerManager = APIUtil.getPlayerManager();
        if (playerManager == null) {
            logger.error("Player manager could not be fetched.");
            return;
        }

        playerManager.getPlayerCache().invalidate(packet.getUuid());

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        for (StorageService service : cachedConfig.getStorage()) {
            PlayerModel model = new PlayerModel();
            model.setUuid(packet.getUuid());
            service.deleteModel(model);
        }
    }

    private void handleMulti(@NotNull MultiPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling multi-packet.");
        }

        for (Packet p : packet.getPackets()) {
            handleGenericPacket(p);
        }
    }

    private void handleGenericPacket(@NotNull Packet packet) {
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

    private boolean isDuplicate(@NotNull UUID messageId) {
        if (Boolean.TRUE.equals(messageCache.getIfPresent(messageId))) {
            return true;
        }
        // Double-checked locking
        synchronized (messageCacheLock) {
            if (Boolean.TRUE.equals(messageCache.getIfPresent(messageId))) {
                return true;
            }
            messageCache.put(messageId, Boolean.TRUE);
        }
        return false;
    }
}
