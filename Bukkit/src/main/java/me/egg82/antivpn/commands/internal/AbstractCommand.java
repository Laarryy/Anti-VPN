package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import co.aikar.taskchain.TaskChainFactory;
import java.io.IOException;
import java.util.UUID;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import me.egg82.antivpn.services.lookup.PlayerInfo;
import me.egg82.antivpn.services.lookup.PlayerLookup;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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

    protected final <T> @Nullable T handleException(@NonNull Throwable ex) {
        if (ex instanceof APIException) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage(), ex);
            } else {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage());
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error(ex.getMessage(), ex);
            } else {
                logger.error(ex.getMessage());
            }
        }
        return null;
    }

    protected @Nullable UUID fetchUuid(@NonNull String name) {
        PlayerInfo info;
        try {
            info = PlayerLookup.get(name);
        } catch (IOException ex) {
            logger.warn("Could not fetch player UUID. (rate-limited?)", ex);
            return null;
        }
        return info.getUUID();
    }

    protected final TaskChainAbortAction<?, ?, ?> handleAbort = new TaskChainAbortAction<Object, Object, Object>() {
        public void onAbort(TaskChain<?> chain, Object arg1, Object arg2, Object arg3) {
            issuer.sendError(Message.ERROR__INTERNAL);
        }
    };
}
