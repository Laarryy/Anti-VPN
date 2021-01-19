package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import co.aikar.taskchain.TaskChainFactory;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCommand implements Runnable {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final CommandIssuer issuer;
    protected final TaskChainFactory taskFactory;

    protected AbstractCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory) {
        this.issuer = issuer;
        this.taskFactory = taskFactory;
    }

    protected @NonNull CompletableFuture<UUID> fetchUuid(@NonNull String name) { return PlayerLookup.get(name).thenApply(PlayerInfo::getUUID); }

    protected final TaskChainAbortAction<?, ?, ?> handleAbort = new TaskChainAbortAction<Object, Object, Object>() {
        public void onAbort(TaskChain<?> chain, Object arg1, Object arg2, Object arg3) {
            issuer.sendError(Message.ERROR__INTERNAL);
        }
    };
}
