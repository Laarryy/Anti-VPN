package me.egg82.antivpn.api.model.ip;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericIPManager implements IPManager {
    public @Nullable CompletableFuture<IP> getIp(@NonNull String ip) {

    }

    public @NonNull CompletableFuture<Void> saveIp(@NonNull IP ip) {

    }

    public @NonNull CompletableFuture<Void> deleteIp(@NonNull String ip) {

    }

    public @NonNull CompletableFuture<Set<String>> getIps() {

    }

    public @NonNull AlgorithmMethod getCurrentAlgorithmMethod() {

    }

    public @NonNull CompletableFuture<Boolean> cascade(@NonNull String ip, boolean useCache) {

    }

    public @NonNull CompletableFuture<Double> consensus(@NonNull String ip, boolean useCache) {

    }
}
