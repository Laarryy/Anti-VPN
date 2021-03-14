package me.egg82.antivpn.update;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.utils.VersionUtil;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractUpdater implements Updater {
    protected final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    protected final String currentVersion;
    protected final long checkDelay;

    private String newVersion;
    private final AtomicLong lastCheckTime = new AtomicLong(1L);
    private volatile boolean updateAvailable = false;

    protected AbstractUpdater(@NotNull String currentVersion, @NotNull TimeUtil.Time checkDelay) {
        this.currentVersion = currentVersion;
        this.newVersion = currentVersion;
        this.checkDelay = checkDelay.getMillis();
    }

    @Override
    public @NotNull String getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> isUpdateAvailable() {
        return getLatestVersion().thenApply(v -> updateAvailable);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> getLatestVersion() {
        return CompletableFuture.supplyAsync(() -> {
            long current = System.currentTimeMillis();
            if (lastCheckTime.updateAndGet(v -> current - v >= checkDelay ? current : v) == current) {
                try {
                    newVersion = getNewVersion();
                } catch (IOException ex) {
                    logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }
            if (isNewVersion(newVersion, currentVersion)) {
                updateAvailable = true;
            }
            return newVersion;
        });
    }

    protected abstract @NotNull String getNewVersion() throws IOException;

    private boolean isNewVersion(@NotNull String newVersion, @NotNull String currentVersion) {
        int[] latest = VersionUtil.parseVersion(newVersion, '.');
        int[] current = VersionUtil.parseVersion(currentVersion, '.');

        for (int i = 0; i < latest.length; i++) {
            if (i > current.length) {
                // We're looking for a version deeper than what we have
                // eg. 1.12.2 -> 1.12
                return false;
            }
            if (current[i] > latest[i]) {
                // The version we're at now is greater than the one we want
                // eg. 1.11 -> 1.13
                return false;
            }
            if (current[i] < latest[i]) {
                // The version we're at now is less than the one we want
                // eg. 1.13 -> 1.11
                return true;
            }
        }
        return false;
    }
}
