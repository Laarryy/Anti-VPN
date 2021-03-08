package me.egg82.antivpn.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.objects.Object2ByteArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ByteMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;

public class CollectionProvider {
    private CollectionProvider() { }

    private static final Object2ByteMap<UUID> serverVersions = new Object2ByteArrayMap<>();
    public static Object2ByteMap<UUID> getServerVersions() { return serverVersions; }

    private static final Cache<UUID, Boolean> messageCache = Caffeine.newBuilder()
        .expireAfterWrite(2L, TimeUnit.MINUTES)
        .expireAfterAccess(30L, TimeUnit.SECONDS)
        .build();
    public static Cache<UUID, Boolean> getMessageCache() { return messageCache; }

    public static boolean isDuplicateMessage(@NotNull UUID messageId) {
        AtomicBoolean retVal = new AtomicBoolean(true);
        messageCache.get(messageId, k -> {
            retVal.set(false);
            return Boolean.FALSE;
        });
        return retVal.get();
    }
}
