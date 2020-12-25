package me.egg82.antivpn;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.util.concurrent.AtomicDouble;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.messaging.Messaging;
import me.egg82.antivpn.messaging.MessagingException;
import me.egg82.antivpn.services.StorageMessagingHandler;
import me.egg82.antivpn.storage.Storage;
import me.egg82.antivpn.storage.StorageException;
import me.egg82.antivpn.storage.results.MCLeaksResult;
import me.egg82.antivpn.storage.results.PostMCLeaksResult;
import me.egg82.antivpn.storage.results.PostVPNResult;
import me.egg82.antivpn.storage.results.VPNResult;
import me.egg82.antivpn.utils.ValidationUtil;
import me.gong.mcleaks.MCLeaksAPI;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VPNAPI {
    private static final Logger logger = LoggerFactory.getLogger(VPNAPI.class);

    private static final VPNAPI api = new VPNAPI();

    private static final AtomicLong numSentMessages = new AtomicLong(0L);

    private VPNAPI() { }

    public static VPNAPI getInstance() { return api; }

    private static MCLeaksAPI mcleaksAPI = null;
    private static LoadingCache<UUID, Boolean> mcleaksCache = null;
    private static LoadingCache<String, Boolean> cascadeCache = null;
    private static LoadingCache<String, Double> consensusCache = null;
    private static LoadingCache<String, Boolean> sourceValidationCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build(k -> Boolean.TRUE);

    public static void reload() {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            logger.error("Could not get configuration.");
            return;
        }
        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            logger.error("Cached config could not be fetched.");
            return;
        }

        mcleaksCache = Caffeine.newBuilder().expireAfterAccess(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).expireAfterWrite(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).build(VPNAPI::mcleaksExpensive);
        cascadeCache = Caffeine.newBuilder().expireAfterAccess(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).expireAfterWrite(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).build(VPNAPI::cascadeExpensive);
        consensusCache = Caffeine.newBuilder().expireAfterAccess(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).expireAfterWrite(cachedConfig.get().getCacheTime().getTime(), cachedConfig.get().getCacheTime().getUnit()).build(VPNAPI::consensusExpensive);

        if (mcleaksAPI != null) {
            mcleaksAPI.shutdown();
        }

        mcleaksAPI = MCLeaksAPI.builder()
                .nocache()
                .threadCount(cachedConfig.get().getThreads())
                .userAgent("egg82/AntiVPN")
                .apiKey(config.get().getNode("mcleaks", "key").getString(""))
                .build();
    }

    public static void close() {
        if (mcleaksAPI != null) {
            mcleaksAPI.shutdown();
        }
    }

    public Map<String, Optional<Boolean>> testAllSources(String ip) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(false, "Could not get cached config.");
        }

        ExecutorService threadPool = Executors.newWorkStealingPool(cachedConfig.get().getThreads());
        CountDownLatch latch = new CountDownLatch(cachedConfig.get().getSources().size());
        Map<String, Optional<Boolean>> retVal;
        ConcurrentMap<String, Optional<Boolean>> results = new ConcurrentHashMap<>();
        for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
            threadPool.submit(() -> {
                if (cachedConfig.get().getDebug()) {
                    logger.info("Getting VPN result from " + kvp.getKey());
                }
                try {
                    results.put(kvp.getKey(), Optional.of(kvp.getValue().getResult(ip)));
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                    results.put(kvp.getKey(), Optional.empty());
                }
                latch.countDown();
            });
        }

        try {
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                logger.warn("Timeout reached before all sources could be queried.");
            }
        } catch (InterruptedException ex) {
            if (cachedConfig.get().getDebug()) {
                logger.error(ex.getMessage(), ex);
            } else {
                logger.error(ex.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        threadPool.shutdownNow(); // Kill it with fire

        // Re-order sources
        retVal = new LinkedHashMap<>();
        for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
            retVal.put(kvp.getKey(), results.get(kvp.getKey()) == null ? Optional.empty() : results.get(kvp.getKey()));
        }

        return retVal;
    }

    public boolean getSourceResult(String ip, String sourceName) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }
        if (sourceName == null) {
            throw new APIException(false, "sourceName cannot be null.");
        }

        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(false, "Could not get cached config.");
        }

        SourceAPI source = null;
        for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
            if (sourceName.equalsIgnoreCase(kvp.getKey())) {
                source = kvp.getValue();
                break;
            }
        }
        if (source == null) {
            throw new APIException(false, "Could not get source from name provided.");
        }

        return source.getResult(ip);
    }

    public boolean cascade(String ip) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        if (cascadeCache == null) {
            throw new APIException(false, "API not yet initialized.");
        }

        Boolean value = cascadeCache.get(ip);
        if (value == null) {
            throw new APIException(false, "Could not get VPN result.");
        }
        return value;
    }

    public double consensus(String ip) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        if (consensusCache == null) {
            throw new APIException(false, "API not yet initialized.");
        }

        Double value = consensusCache.get(ip);
        if (value == null) {
            throw new APIException(false, "Could not get VPN result.");
        }
        return value;
    }

    public boolean isMCLeaks(UUID playerID) throws APIException {
        if (playerID == null) {
            throw new APIException(false, "playerID cannot be null.");
        }

        if (mcleaksAPI == null || mcleaksCache == null) {
            throw new APIException(false, "API not yet initialized.");
        }

        Boolean value = mcleaksCache.get(playerID);
        if (value == null) {
            throw new APIException(false, "Could not get MCLeaks result.");
        }
        return value;
    }

    public long getNumSentMessages() throws APIException { return numSentMessages.get(); }

    public long getNumReceivedMessages() throws APIException {
        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            throw new APIException(false, "Could not get handler service.");
        }

        return handler.numReceivedMessages();
    }

    private static boolean cascadeExpensive(String ip) throws APIException {
        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(false, "Could not get cached config.");
        }

        VPNResult result = null;
        for (Storage s : cachedConfig.get().getStorage()) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Getting VPN result from " + s.getClass().getSimpleName());
            }
            try {
                result = s.getVPNByIP(ip, cachedConfig.get().getSourceCacheTime());
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }
        if (result != null && result.getCascade().isPresent()) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Got VPN result: " + ip + " = " + result.getCascade().get());
            }
            return result.getCascade().get();
        }

        Optional<Boolean> r = Optional.empty();
        boolean isHard = true;
        for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
            if (!sourceValidationCache.get(kvp.getKey())) {
                if (cachedConfig.get().getDebug()) {
                    logger.info("Skipping " + kvp.getKey() + " due to recently bad/failed result.");
                }
                continue;
            }
            if (cachedConfig.get().getDebug()) {
                logger.info("Getting VPN result from " + kvp.getKey());
            }
            try {
                r = Optional.of(kvp.getValue().getResult(ip));
                if (cachedConfig.get().getDebug()) {
                    logger.info(kvp.getKey() + " returned " + r.get() + " for " + ip);
                }
                break;
            } catch (APIException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                }
                if (!ex.isHard()) {
                    isHard = false;
                }

                if (cachedConfig.get().getDebug()) {
                    logger.info(kvp.getKey() + " returned a bad/failed result. Skipping source for a while.");
                }
                sourceValidationCache.put(kvp.getKey(), Boolean.FALSE);
            }
        }
        if (!r.isPresent()) {
            throw new APIException(isHard, "Cascade had no valid/usable sources.");
        }

        boolean value = r.get();

        if (cachedConfig.get().getDebug()) {
            logger.info("Got VPN result: " + ip + " = " + value);
            logger.info("Propagating to storage & messaging");
        }

        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            throw new APIException(false, "Could not get handler service.");
        }

        PostVPNResult postResult = null;
        Storage postedStorage = null;
        boolean canRecover = false;
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                postResult = s.postVPN(ip, value);
                postedStorage = s;
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
                if (ex.isAutomaticallyRecoverable()) {
                    canRecover = true;
                }
            }
        }
        if (postResult == null) {
            throw new APIException(!canRecover, "Could not put VPN in storage.");
        }

        handler.cacheVPNPost(postResult.getID());
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                if (s == postedStorage) {
                    continue;
                }
                s.postVPNRaw(
                        postResult.getID(),
                        postResult.getIPID(),
                        postResult.getCascade(),
                        postResult.getConsensus(),
                        postResult.getCreated()
                );
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }

        canRecover = false;
        if (cachedConfig.get().getMessaging().size() > 0) {
            boolean handled = false;
            UUID messageID = UUID.randomUUID();
            handler.cacheMessage(messageID);
            for (Messaging m : cachedConfig.get().getMessaging()) {
                try {
                    m.sendPostVPN(
                            messageID,
                            postResult.getID(),
                            postResult.getIPID(),
                            postResult.getIP(),
                            postResult.getCascade(),
                            postResult.getConsensus(),
                            postResult.getCreated()
                    );
                    handled = true;
                } catch (MessagingException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                    }
                    if (ex.isAutomaticallyRecoverable()) {
                        canRecover = true;
                    }
                }
            }

            if (!handled) {
                throw new APIException(!canRecover, "Could not send VPN through messaging.");
            }
        }

        numSentMessages.getAndIncrement();
        return value;
    }

    private static double consensusExpensive(String ip) throws APIException {
        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(false, "Could not get cached config.");
        }

        VPNResult result = null;
        for (Storage s : cachedConfig.get().getStorage()) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Getting VPN result from " + s.getClass().getSimpleName());
            }
            try {
                result = s.getVPNByIP(ip, cachedConfig.get().getSourceCacheTime());
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }
        if (result != null && result.getConsensus().isPresent()) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Got VPN result: " + ip + " = " + result.getConsensus().get());
            }
            return result.getConsensus().get();
        }

        ExecutorService threadPool = Executors.newWorkStealingPool(cachedConfig.get().getThreads());
        CountDownLatch latch = new CountDownLatch(cachedConfig.get().getSources().size());
        AtomicDouble r = new AtomicDouble(0.0d);
        AtomicDouble success = new AtomicDouble(0.0d);
        AtomicBoolean isHard = new AtomicBoolean(true);
        for (Map.Entry<String, SourceAPI> kvp : cachedConfig.get().getSources().entrySet()) {
            threadPool.submit(() -> {
                if (!sourceValidationCache.get(kvp.getKey())) {
                    if (cachedConfig.get().getDebug()) {
                        logger.info("Skipping " + kvp.getKey() + " due to recently bad/failed result.");
                    }
                    latch.countDown();
                    return;
                }
                if (cachedConfig.get().getDebug()) {
                    logger.info("Getting VPN result from " + kvp.getKey());
                }
                try {
                    boolean tmp = kvp.getValue().getResult(ip);
                    r.getAndAdd(tmp ? 1.0d : 0.0d);
                    success.getAndAdd(1.0d);
                    if (cachedConfig.get().getDebug()) {
                        logger.info(kvp.getKey() + " returned " + tmp + " for " + ip);
                    }
                } catch (APIException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Hard: " + ex.isHard() + "] " + ex.getMessage());
                    }
                    if (!ex.isHard()) {
                        isHard.set(false);
                    }

                    if (cachedConfig.get().getDebug()) {
                        logger.info(kvp.getKey() + " returned a bad/failed result. Skipping source for a while.");
                    }
                    sourceValidationCache.put(kvp.getKey(), Boolean.FALSE);
                }
                latch.countDown();
            });
        }

        try {
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                logger.warn("Timeout reached before all sources could be queried.");
            }
        } catch (InterruptedException ex) {
            if (cachedConfig.get().getDebug()) {
                logger.error(ex.getMessage(), ex);
            } else {
                logger.error(ex.getMessage());
            }
            Thread.currentThread().interrupt();
        }
        threadPool.shutdownNow(); // Kill it with fire

        if (success.get() == 0.0d) {
            throw new APIException(isHard.get(), "Consensus had no valid/usable sources.");
        }

        double value = r.get() / success.get();

        if (cachedConfig.get().getDebug()) {
            logger.info("Got VPN result: " + ip + " = " + value);
            logger.info("Propagating to storage & messaging");
        }

        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            throw new APIException(false, "Could not get handler service.");
        }

        PostVPNResult postResult = null;
        Storage postedStorage = null;
        boolean canRecover = false;
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                postResult = s.postVPN(ip, value);
                postedStorage = s;
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
                if (ex.isAutomaticallyRecoverable()) {
                    canRecover = true;
                }
            }
        }
        if (postResult == null) {
            throw new APIException(!canRecover, "Could not put VPN in storage.");
        }

        handler.cacheVPNPost(postResult.getID());
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                if (s == postedStorage) {
                    continue;
                }
                s.postVPNRaw(
                        postResult.getID(),
                        postResult.getIPID(),
                        postResult.getCascade(),
                        postResult.getConsensus(),
                        postResult.getCreated()
                );
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }

        canRecover = false;
        if (cachedConfig.get().getMessaging().size() > 0) {
            boolean handled = false;
            UUID messageID = UUID.randomUUID();
            handler.cacheMessage(messageID);
            for (Messaging m : cachedConfig.get().getMessaging()) {
                try {
                    m.sendPostVPN(
                            messageID,
                            postResult.getID(),
                            postResult.getIPID(),
                            postResult.getIP(),
                            postResult.getCascade(),
                            postResult.getConsensus(),
                            postResult.getCreated()
                    );
                    handled = true;
                } catch (MessagingException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                    }
                    if (ex.isAutomaticallyRecoverable()) {
                        canRecover = true;
                    }
                }
            }

            if (!handled) {
                throw new APIException(!canRecover, "Could not send VPN through messaging.");
            }
        }

        numSentMessages.getAndIncrement();
        return value;
    }

    private static boolean mcleaksExpensive(UUID playerID) throws APIException {
        Optional<CachedConfig> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(false, "Could not get cached config.");
        }

        MCLeaksResult result = null;
        for (Storage s : cachedConfig.get().getStorage()) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Getting MCLeaks result from " + s.getClass().getSimpleName());
            }
            try {
                result = s.getMCLeaksByPlayer(playerID, cachedConfig.get().getMCLeaksCacheTime());
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }
        if (result != null) {
            if (cachedConfig.get().getDebug()) {
                logger.info("Got MCLeaks result: " + playerID + " = " + result.getValue());
            }
            return result.getValue();
        }

        if (cachedConfig.get().getDebug()) {
            logger.info("Getting MCLeaks result from API");
        }

        MCLeaksAPI.Result apiResult = mcleaksAPI.checkAccount(playerID);
        if (apiResult.hasError()) {
            throw new APIException(false, apiResult.getError());
        }

        boolean value = apiResult.isMCLeaks();

        if (cachedConfig.get().getDebug()) {
            logger.info("Got MCLeaks result: " + playerID + " = " + value);
            logger.info("Propagating to storage & messaging");
        }

        StorageMessagingHandler handler;
        try {
            handler = ServiceLocator.get(StorageMessagingHandler.class);
        } catch (InstantiationException | IllegalAccessException | ServiceNotFoundException ex) {
            throw new APIException(false, "Could not get handler service.");
        }

        PostMCLeaksResult postResult = null;
        Storage postedStorage = null;
        boolean canRecover = false;
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                postResult = s.postMCLeaks(playerID, value);
                postedStorage = s;
                break;
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
                if (ex.isAutomaticallyRecoverable()) {
                    canRecover = true;
                }
            }
        }
        if (postResult == null) {
            throw new APIException(!canRecover, "Could not put MCLeaks in storage.");
        }

        handler.cacheMCLeaksPost(postResult.getID());
        for (Storage s : cachedConfig.get().getStorage()) {
            try {
                if (s == postedStorage) {
                    continue;
                }
                s.postMCLeaksRaw(
                        postResult.getID(),
                        postResult.getLongPlayerID(),
                        postResult.getValue(),
                        postResult.getCreated()
                );
            } catch (StorageException ex) {
                if (cachedConfig.get().getDebug()) {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                } else {
                    logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                }
            }
        }

        canRecover = false;
        if (cachedConfig.get().getMessaging().size() > 0) {
            boolean handled = false;
            UUID messageID = UUID.randomUUID();
            handler.cacheMessage(messageID);
            for (Messaging m : cachedConfig.get().getMessaging()) {
                try {
                    m.sendPostMCLeaks(
                            messageID,
                            postResult.getID(),
                            postResult.getLongPlayerID(),
                            postResult.getPlayerID(),
                            postResult.getValue(),
                            postResult.getCreated()
                    );
                    handled = true;
                } catch (MessagingException ex) {
                    if (cachedConfig.get().getDebug()) {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage(), ex);
                    } else {
                        logger.error("[Recoverable: " + ex.isAutomaticallyRecoverable() + "] " + ex.getMessage());
                    }
                    if (ex.isAutomaticallyRecoverable()) {
                        canRecover = true;
                    }
                }
            }

            if (!handled) {
                throw new APIException(!canRecover, "Could not send MCLeaks through messaging.");
            }
        }

        numSentMessages.getAndIncrement();
        return value;
    }
}
