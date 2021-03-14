package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ImportCommand extends AbstractCommand {
    private final String masterName;
    private final String slaveName;
    private final String batchMax;

    public ImportCommand(@NotNull ProxyServer proxy, @NotNull CommandIssuer issuer, @NotNull String masterName, @NotNull String slaveName, @NotNull String batchMax) {
        super(proxy, issuer);
        this.masterName = masterName;
        this.slaveName = slaveName;
        this.batchMax = batchMax;
    }

    @Override
    public void run() {
        if (masterName.isEmpty()) {
            issuer.sendError(MessageKey.IMPORT__NO_MASTER);
            return;
        }
        if (slaveName.isEmpty()) {
            issuer.sendError(MessageKey.IMPORT__NO_SLAVE);
            return;
        }

        if (masterName.equalsIgnoreCase(slaveName)) {
            issuer.sendError(MessageKey.IMPORT__SAME_STORAGE);
            return;
        }

        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

        int max = batchMax == null ? 50 : Integer.parseInt(batchMax);

        int masterIndex = -1;
        int slaveIndex = -1;
        for (int i = 0; i < cachedConfig.getStorage().size(); i++) {
            StorageService storage = cachedConfig.getStorage().get(i);
            if (masterIndex == -1 && masterName.equalsIgnoreCase(storage.getName())) {
                masterIndex = i;
            } else if (slaveIndex == -1 && slaveName.equalsIgnoreCase(storage.getName())) { // The elseif helps to ensure we aren't trying to import from master to master
                slaveIndex = i;
            }
        }

        if (masterIndex == -1) {
            issuer.sendError(MessageKey.IMPORT__NO_MASTER);
            return;
        }
        if (slaveIndex == -1) {
            issuer.sendError(MessageKey.IMPORT__NO_SLAVE);
            return;
        }

        issuer.sendInfo(MessageKey.IMPORT__BEGIN);

        StorageService master = cachedConfig.getStorage().get(masterIndex);
        StorageService slave = cachedConfig.getStorage().get(slaveIndex);

        issuer.sendInfo(MessageKey.IMPORT__IPS, "{id}", "0");
        int start = 1;
        Set<IPModel> ipModels;
        do {
            ipModels = master.getAllIps(start, max);
            slave.storeModels(ipModels);
            issuer.sendInfo(MessageKey.IMPORT__IPS, "{id}", String.valueOf(start + ipModels.size()));
            start += ipModels.size();
        } while (ipModels.size() == max);

        issuer.sendInfo(MessageKey.IMPORT__PLAYERS, "{id}", "0");
        start = 1;
        Set<PlayerModel> playerModels;
        do {
            playerModels = master.getAllPlayers(start, max);
            slave.storeModels(playerModels);
            issuer.sendInfo(MessageKey.IMPORT__PLAYERS, "{id}", String.valueOf(start + playerModels.size()));
            start += playerModels.size();
        } while (playerModels.size() == max);

        issuer.sendInfo(MessageKey.IMPORT__END);
    }
}
