package me.egg82.antivpn.messaging.handler;

import me.egg82.antivpn.api.VPNAPIImpl;
import me.egg82.antivpn.api.model.ip.AbstractIPManager;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.player.AbstractPlayerManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.messaging.packets.Packet;
import me.egg82.antivpn.messaging.packets.vpn.DeleteIPPacket;
import me.egg82.antivpn.messaging.packets.vpn.DeletePlayerPacket;
import me.egg82.antivpn.messaging.packets.vpn.IPPacket;
import me.egg82.antivpn.messaging.packets.vpn.PlayerPacket;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.jetbrains.annotations.NotNull;

public class VPNMessagingHandler extends AbstractMessagingHandler {
    protected boolean handlePacket(@NotNull Packet packet) {
        if (packet instanceof IPPacket) {
            handleIp((IPPacket) packet);
            return true;
        } else if (packet instanceof DeleteIPPacket) {
            handleDeleteIp((DeleteIPPacket) packet);
            return true;
        } else if (packet instanceof PlayerPacket) {
            handlePlayer((PlayerPacket) packet);
            return true;
        } else if (packet instanceof DeletePlayerPacket) {
            handleDeletePlayer((DeletePlayerPacket) packet);
            return true;
        }

        return false;
    }

    private void handleIp(@NotNull IPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling packet for " + packet.getIp() + ".");
        }

        VPNAPIImpl api = VPNAPIImpl.get();
        AbstractIPManager ipManager = api != null ? api.getIPManager() : null;
        if (ipManager == null) {
            logger.error("IP manager could not be fetched.");
            return;
        }

        IPModel m = new IPModel();
        m.setIp(packet.getIp());
        m.setType(packet.getType().ordinal());
        m.setCascade(packet.getCascade());
        m.setConsensus(packet.getConsensus());
        ipManager.getIpCache().put(new Pair<>(packet.getIp(), packet.getType()), m);

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        for (StorageService service : cachedConfig.getStorage()) {
            IPModel model = service.getOrCreateIpModel(packet.getIp(), packet.getType().ordinal());
            model.setCascade(packet.getCascade());
            model.setConsensus(packet.getConsensus());
            service.storeModel(model);
        }
    }

    private void handleDeleteIp(@NotNull DeleteIPPacket packet) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Handling deletion packet for " + packet.getIp() + ".");
        }

        VPNAPIImpl api = VPNAPIImpl.get();
        AbstractIPManager ipManager = api != null ? api.getIPManager() : null;
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

        VPNAPIImpl api = VPNAPIImpl.get();
        AbstractPlayerManager playerManager = api != null ? api.getPlayerManager() : null;
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

        VPNAPIImpl api = VPNAPIImpl.get();
        AbstractPlayerManager playerManager = api != null ? api.getPlayerManager() : null;
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
}
