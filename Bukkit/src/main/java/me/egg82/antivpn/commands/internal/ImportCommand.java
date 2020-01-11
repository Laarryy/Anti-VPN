package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import java.util.Optional;
import java.util.Set;
import me.egg82.antivpn.core.PlayerResult;
import me.egg82.antivpn.enums.Message;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.storage.StorageException;
import me.egg82.antivpn.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer issuer;
    private final String masterName;
    private final String slaveName;
    private final String batchMax;
    private final TaskChain<?> chain;

    public ImportCommand(CommandIssuer issuer, String masterName, String slaveName, String batchMax, TaskChain<?> chain) {
        this.issuer = issuer;
        this.masterName = masterName;
        this.slaveName = slaveName;
        this.batchMax = batchMax;
        this.chain = chain;
    }

    public void run() {
        if (masterName == null || masterName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_MASTER);
            return;
        }
        if (slaveName == null || slaveName.isEmpty()) {
            issuer.sendError(Message.IMPORT__NO_SLAVE);
            return;
        }

        if (masterName.equalsIgnoreCase(slaveName)) {
            issuer.sendError(Message.IMPORT__SAME_STORAGE);
            return;
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

        int max = batchMax == null ? 50 : Integer.parseInt(batchMax);

        int masterIndex = -1;
        int slaveIndex = -1;
        for (int i = 0; i < cachedConfig.get().getStorage().size(); i++) {
            Storage storage = cachedConfig.get().getStorage().get(i);
            if (masterIndex == -1 && masterName.equalsIgnoreCase(storage.getClass().getSimpleName())) {
                masterIndex = i;
            } else if (slaveIndex == -1 && slaveName.equalsIgnoreCase(storage.getClass().getSimpleName())) { // The elseif helps to ensure we aren't trying to import from master to master
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

        Storage master = cachedConfig.get().getStorage().get(masterIndex);
        Storage slave = cachedConfig.get().getStorage().get(slaveIndex);

        chain
                .sync(() -> issuer.sendInfo(Message.IMPORT__LEVELS))
                .<Boolean>asyncCallback((v, f) -> {
                    try {
                        slave.loadLevels(master.dumpLevels());
                    } catch (StorageException ex) {
                        logger.error("Could not import levels.", ex);
                        f.accept(null);
                        return;
                    }
                    f.accept(Boolean.TRUE);
                })
                .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                    public void onAbort(TaskChain<?> chain, Object arg1) {
                        issuer.sendError(Message.ERROR__INTERNAL);
                    }
                })
                .sync(() -> issuer.sendInfo(Message.IMPORT__SERVERS))
                .<Boolean>asyncCallback((v, f) -> {
                    try {
                        slave.loadServers(master.dumpServers());
                    } catch (StorageException ex) {
                        logger.error("Could not import servers.", ex);
                        f.accept(null);
                        return;
                    }
                    f.accept(Boolean.TRUE);
                })
                .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                    public void onAbort(TaskChain<?> chain, Object arg1) {
                        issuer.sendError(Message.ERROR__INTERNAL);
                    }
                })
                .sync(() -> issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", "0"))
                .<Boolean>asyncCallback((v, f) -> {
                    long start = 1L;
                    Set<PlayerResult> players;
                    do {
                        try {
                            players = master.dumpPlayers(start, max);
                            slave.loadPlayers(players, start == 1L);
                        } catch (StorageException ex) {
                            logger.error("Could not import players.", ex);
                            f.accept(null);
                            return;
                        }
                        issuer.sendInfo(Message.IMPORT__PLAYERS, "{id}", String.valueOf(start + players.size()));
                        start += max;
                    } while (players.size() == max);
                    f.accept(Boolean.TRUE);
                })
                .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                    public void onAbort(TaskChain<?> chain, Object arg1) {
                        issuer.sendError(Message.ERROR__INTERNAL);
                    }
                })
                .sync(() -> issuer.sendInfo(Message.IMPORT__CHAT, "{id}", "0"))
                .<Boolean>asyncCallback((v, f) -> {
                    long start = 1L;
                    Set<RawChatResult> chat;
                    do {
                        try {
                            chat = master.dumpChat(start, max);
                            slave.loadChat(chat, start == 1L);
                        } catch (StorageException ex) {
                            logger.error("Could not import chat.", ex);
                            f.accept(null);
                            return;
                        }
                        issuer.sendInfo(Message.IMPORT__CHAT, "{id}", String.valueOf(start + chat.size()));
                        start += max;
                    } while (chat.size() == max);
                    f.accept(Boolean.TRUE);
                })
                .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                    public void onAbort(TaskChain<?> chain, Object arg1) {
                        issuer.sendError(Message.ERROR__INTERNAL);
                    }
                })
                .sync(() -> issuer.sendInfo(Message.IMPORT__END))
                .execute();
    }
}
