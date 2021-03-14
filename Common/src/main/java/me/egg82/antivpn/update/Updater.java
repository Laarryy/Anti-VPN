package me.egg82.antivpn.update;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;

public interface Updater {
    @NotNull String getCurrentVersion();

    @NotNull CompletableFuture<@NotNull Boolean> isUpdateAvailable();

    @NotNull CompletableFuture<@NotNull String> getLatestVersion();

    @NotNull String getDownloadLink();
}
