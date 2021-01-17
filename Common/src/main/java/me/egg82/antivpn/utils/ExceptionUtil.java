package me.egg82.antivpn.utils;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.config.ConfigUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

public class ExceptionUtil {
    private ExceptionUtil() { }

    public static void handleException(@NonNull Throwable ex, @NonNull Logger logger) {
        Throwable oldEx = null;
        if (ex instanceof CompletionException || ex instanceof ExecutionException) {
            oldEx = ex;
            ex = ex.getCause();
        }
        while (ex instanceof CompletionException || ex instanceof ExecutionException) {
            ex = ex.getCause();
        }

        if (ex instanceof APIException) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage(), oldEx != null ? oldEx : ex);
            } else {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage());
            }
        } else {
            if (ex instanceof CancellationException) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.warn(ex.getMessage(), oldEx != null ? oldEx : ex);
                } else {
                    logger.warn(ex.getMessage());
                }
            } else {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getMessage(), oldEx != null ? oldEx : ex);
                } else {
                    logger.error(ex.getMessage());
                }
            }
        }
    }
}
