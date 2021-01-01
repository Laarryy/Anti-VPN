package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.util.*;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ImportCommand extends AbstractCommand {
    private final String masterName;
    private final String slaveName;
    private final String batchMax;

    public ImportCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull String masterName, @NonNull String slaveName, @NonNull String batchMax) {
        super(issuer, taskFactory);
        this.masterName = masterName;
        this.slaveName = slaveName;
        this.batchMax = batchMax;
    }

    public void run() {
        if (masterName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_MASTER);
            return;
        }
        if (slaveName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_SLAVE);
            return;
        }

        if (masterName.equalsIgnoreCase(slaveName)) {
            issuer.sendError(Message.IMPORT__SAME_STORAGE);
            return;
        }

       CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

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
            issuer.sendError(Message.IMPORT__NO_MASTER);
            return;
        }
        if (slaveIndex == -1) {
            issuer.sendError(Message.IMPORT__NO_SLAVE);
            return;
        }

        issuer.sendInfo(Message.IMPORT__BEGIN);

        StorageService master = cachedConfig.getStorage().get(masterIndex);
        StorageService slave = cachedConfig.getStorage().get(slaveIndex);

        taskFactory.<Void>newChain()
                .sync(() -> issuer.sendInfo(Message.IMPORT__IPS, "{id}", "0"))
                .<Integer>asyncCallback((v, r) -> {
                    int start = 1;
                    Set<IPModel> models;
                    do {
                        models = master.getAllIps(start, max);
                        slave.storeModels(models);
                        issuer.sendInfo(Message.IMPORT__IPS, "{id}", String.valueOf(start + models.size()));
                        start += max;
                    } while (models.size() == max);
                    r.accept(start);
                })
                .sync(() -> issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", "0"))
                .<Integer>asyncCallback((v, r) -> {
                    int start = 1;
                    Set<PlayerModel> models;
                    do {
                        models = master.getAllPlayers(start, max);
                        slave.storeModels(models);
                        issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", String.valueOf(start + models.size()));
                        start += max;
                    } while (models.size() == max);
                    r.accept(start);
                })
                .syncLast(v -> issuer.sendInfo(Message.IMPORT__END))
                .execute();
    }
}
