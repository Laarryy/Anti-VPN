package me.egg82.antivpn.commands.internal;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.paper.PaperCommandManager;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

public class ImportCommand extends AbstractCommand {
    public ImportCommand(@NotNull PaperCommandManager<BukkitLocalizedCommandSender> commandManager) {
        super(commandManager);
    }

    @Override
    public void execute(@NonNull CommandContext<BukkitLocalizedCommandSender> commandContext) { }

    /*private final String masterName;
    private final String slaveName;
    private final String batchMax;

    public ImportCommand(@NotNull CommandIssuer issuer, @NotNull TaskChainFactory taskFactory, @NotNull String masterName, @NotNull String slaveName, @NotNull String batchMax) {
        super(issuer, taskFactory);
        this.masterName = masterName;
        this.slaveName = slaveName;
        this.batchMax = batchMax;
    }

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

        TaskChain<Void> chain = taskFactory.newChain();
        chain.setErrorHandler((ex, task) -> ExceptionUtil.handleException(ex, logger));
        chain
                .sync(() -> issuer.sendInfo(MessageKey.IMPORT__IPS, "{id}", "0"))
                .<Integer>asyncCallback((v, r) -> {
                    int start = 1;
                    Set<IPModel> models;
                    do {
                        models = master.getAllIps(start, max);
                        slave.storeModels(models);
                        issuer.sendInfo(MessageKey.IMPORT__IPS, "{id}", String.valueOf(start + models.size()));
                        start += models.size();
                    } while (models.size() == max);
                    r.accept(start);
                })
                .sync(() -> issuer.sendInfo(MessageKey.IMPORT__PLAYERS, "{id}", "0"))
                .<Integer>asyncCallback((v, r) -> {
                    int start = 1;
                    Set<PlayerModel> models;
                    do {
                        models = master.getAllPlayers(start, max);
                        slave.storeModels(models);
                        issuer.sendInfo(MessageKey.IMPORT__PLAYERS, "{id}", String.valueOf(start + models.size()));
                        start += models.size();
                    } while (models.size() == max);
                    r.accept(start);
                })
                .syncLast(v -> issuer.sendInfo(MessageKey.IMPORT__END))
                .execute();
    }*/
}
