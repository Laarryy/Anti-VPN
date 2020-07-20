package me.egg82.antivpn.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.egg82.antivpn.core.MCLeaksResult;
import me.egg82.antivpn.core.VPNResult;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.messaging.Messaging;
import me.egg82.antivpn.messaging.MessagingException;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.storage.StorageException;
import me.egg82.antivpn.utils.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class StorageMessagingHandler implements StorageHandler, MessagingHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<UUID, Boolean> cachedMessages = Caffeine.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).expireAfterWrite(10L, TimeUnit.MINUTES).build(k -> Boolean.FALSE);
    private final LoadingCache<Long, Boolean> cachedVPNPosts = Caffeine.newBuilder().expireAfterAccess(2L, TimeUnit.MINUTES).expireAfterWrite(5L, TimeUnit.MINUTES).build(k -> Boolean.FALSE);
    private final LoadingCache<Long, Boolean> cachedMCLeaksPosts = Caffeine.newBuilder().expireAfterAccess(2L, TimeUnit.MINUTES).expireAfterWrite(5L, TimeUnit.MINUTES).build(k -> Boolean.FALSE);

    private final ExecutorService workPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setNameFormat("AntiVPN-SMH-%d").build());

    private final AtomicLong receivedMessages = new AtomicLong(0L);

    public StorageMessagingHandler() {
        workPool.execute(this::getQueues);
    }

    public void cacheMessage(UUID uuid) { cachedMessages.put(uuid, Boolean.TRUE); }

    public void cacheVPNPost(long id) { cachedVPNPosts.put(id, Boolean.TRUE); }

    public void cacheMCLeaksPost(long id) { cachedMCLeaksPosts.put(id, Boolean.TRUE); }

    public long numReceivedMessages() { return receivedMessages.get(); }

    public void close() {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private void getQueues() {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            try {
                Thread.sleep(10L * 1000L);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            try {
                workPool.execute(this::getQueues);
            } catch (RejectedExecutionException ignored) { }
            return;
        }

        Set<VPNResult> vpnQueue = new LinkedHashSet<>();

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                vpnQueue.addAll(storage.getVPNQueue());
            } catch (StorageException ex) {
                logger.error("Could not get VPN queue from " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Iterator<VPNResult> i = vpnQueue.iterator(); i.hasNext();) {
            VPNResult r = i.next();
            if (cachedVPNPosts.get(r.getID())) {
                i.remove();
                continue;
            }
            cachedVPNPosts.put(r.getID(), Boolean.TRUE);
            receivedMessages.getAndIncrement();
        }

        Set<MCLeaksResult> mcleaksQueue = new LinkedHashSet<>();

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                mcleaksQueue.addAll(storage.getMCLeaksQueue());
            } catch (StorageException ex) {
                logger.error("Could not get MCLeaks queue from " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Iterator<MCLeaksResult> i = mcleaksQueue.iterator(); i.hasNext();) {
            MCLeaksResult r = i.next();
            if (cachedMCLeaksPosts.get(r.getID())) {
                i.remove();
                continue;
            }
            cachedMCLeaksPosts.put(r.getID(), Boolean.TRUE);
            receivedMessages.getAndIncrement();
        }

        try {
            Thread.sleep(10L * 1000L);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        try {
            workPool.execute(this::getQueues);
        } catch (RejectedExecutionException ignored) { }
    }

    public void ipIDCreationCallback(String ip, long longIPID, Storage callingStorage) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("IP created: " + ip + " = " + longIPID);
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            if (storage != callingStorage) {
                try {
                    storage.setIPRaw(longIPID, ip);
                } catch (StorageException ex) {
                    logger.error("Could not set raw IP data for " + storage.getClass().getSimpleName() + ".", ex);
                }
            }
        }

        UUID messageID = UUID.randomUUID();
        cachedMessages.put(messageID, Boolean.TRUE);

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            try {
                messaging.sendIP(messageID, longIPID, ip);
            } catch (MessagingException ex) {
                logger.error("Could not send raw IP data for " + messaging.getClass().getSimpleName() + ".", ex);
            }
        }
    }

    public void playerIDCreationCallback(UUID playerID, long longPlayerID, Storage callingStorage) {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Player created: " + playerID.toString() + " = " + longPlayerID);
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            if (storage != callingStorage) {
                try {
                    storage.setPlayerRaw(longPlayerID, playerID);
                } catch (StorageException ex) {
                    logger.error("Could not set raw player data for " + storage.getClass().getSimpleName() + ".", ex);
                }
            }
        }

        UUID messageID = UUID.randomUUID();
        cachedMessages.put(messageID, Boolean.TRUE);

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            try {
                messaging.sendPlayer(messageID, longPlayerID, playerID);
            } catch (MessagingException ex) {
                logger.error("Could not send raw player data for " + messaging.getClass().getSimpleName() + ".", ex);
            }
        }
    }

    public void ipCallback(UUID messageID, String ip, long longIPID, Messaging callingMessaging) {
        if (cachedMessages.get(messageID)) {
            return;
        }
        cachedMessages.put(messageID, Boolean.TRUE);

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("IP created: " + ip + " = " + longIPID);
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                storage.setIPRaw(longIPID, ip);
            } catch (StorageException ex) {
                logger.error("Could not set raw IP data for " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            if (messaging != callingMessaging) {
                try {
                    messaging.sendIP(messageID, longIPID, ip);
                } catch (MessagingException ex) {
                    logger.error("Could not send raw IP data for " + messaging.getClass().getSimpleName() + ".", ex);
                }
            }
        }
    }

    public void playerCallback(UUID messageID, UUID playerID, long longPlayerID, Messaging callingMessaging) {
        if (cachedMessages.get(messageID)) {
            return;
        }
        cachedMessages.put(messageID, Boolean.TRUE);

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Player created: " + playerID.toString() + " = " + longPlayerID);
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                storage.setPlayerRaw(longPlayerID, playerID);
            } catch (StorageException ex) {
                logger.error("Could not set raw player data for " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            if (messaging != callingMessaging) {
                try {
                    messaging.sendPlayer(messageID, longPlayerID, playerID);
                } catch (MessagingException ex) {
                    logger.error("Could not send raw player data for " + messaging.getClass().getSimpleName() + ".", ex);
                }
            }
        }
    }

    public void postVPNCallback(UUID messageID, long id, long longIPID, String ip, Optional<Boolean> cascade, Optional<Double> consensus, long created, Messaging callingMessaging) {
        if (cachedMessages.get(messageID)) {
            return;
        }
        cachedMessages.put(messageID, Boolean.TRUE);

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("VPN created: " + id + " = \"" + ip + "\" - \"" + cascade.orElse(null) + "\", \"" + consensus.orElse(null) + "\"");
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                storage.postVPNRaw(id, longIPID, cascade, consensus, created);
            } catch (StorageException ex) {
                logger.error("Could not set raw VPN data for " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            if (messaging != callingMessaging) {
                try {
                    messaging.sendPostVPN(messageID, id, longIPID, ip, cascade, consensus, created);
                } catch (MessagingException ex) {
                    logger.error("Could not send raw VPN data for " + messaging.getClass().getSimpleName() + ".", ex);
                }
            }
        }
    }

    public void postMCLeaksCallback(UUID messageID, long id, long longPlayerID, UUID playerID, boolean value, long created, Messaging callingMessaging) {
        if (cachedMessages.get(messageID)) {
            return;
        }
        cachedMessages.put(messageID, Boolean.TRUE);

        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("MCLeaks created: " + id + " = " + playerID.toString() + " - \"" + value + "\"");
            logger.info("Propagating to storage & messaging");
        }

        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        for (Storage storage : cachedConfig.get().getStorage()) {
            try {
                storage.postMCLeaksRaw(id, longPlayerID, value, created);
            } catch (StorageException ex) {
                logger.error("Could not set raw MCLeaks data for " + storage.getClass().getSimpleName() + ".", ex);
            }
        }

        for (Messaging messaging : cachedConfig.get().getMessaging()) {
            if (messaging != callingMessaging) {
                try {
                    messaging.sendPostMCLeaks(messageID, id, longPlayerID, playerID, value, created);
                } catch (MessagingException ex) {
                    logger.error("Could not send raw MCLeaks data for " + messaging.getClass().getSimpleName() + ".", ex);
                }
            }
        }
    }
}
