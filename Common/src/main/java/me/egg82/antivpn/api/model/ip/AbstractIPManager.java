package me.egg82.antivpn.api.model.ip;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.SourceManager;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.messaging.packets.DeleteIPPacket;
import me.egg82.antivpn.messaging.packets.IPPacket;
import me.egg82.antivpn.storage.StorageService;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.utils.PacketUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractIPManager implements IPManager {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final LoadingCache<Pair<String, AlgorithmMethod>, IPModel> ipCache;
    private final LoadingCache<String, Boolean> sourceInvalidationCache;

    private final SourceManager sourceManager;

    protected AbstractIPManager(@NonNull SourceManager sourceManager, long cacheTime, TimeUnit cacheTimeUnit) {
        this.sourceManager = sourceManager;

        ipCache = Caffeine.newBuilder().expireAfterAccess(cacheTime, cacheTimeUnit).expireAfterWrite(cacheTime, cacheTimeUnit).build(k -> calculateIpResult(k.getT1(), k.getT2(), true));
        sourceInvalidationCache = Caffeine.newBuilder().expireAfterWrite(1L, TimeUnit.MINUTES).build(k -> Boolean.FALSE);
    }

    public LoadingCache<Pair<String, AlgorithmMethod>, IPModel> getIpCache() { return ipCache; }

    public @NonNull CompletableFuture<IP> getIP(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            for (StorageService service : cachedConfig.getStorage()) {
                IPModel model = service.getIpModel(ip, cachedConfig.getSourceCacheTime());
                if (model != null) {
                    try {
                        return new GenericIP(InetAddress.getByName(ip), AlgorithmMethod.values()[model.getType()], model.getCascade(), model.getConsensus());
                    } catch (UnknownHostException ex) {
                        throw new IllegalArgumentException("Could not create InetAddress for " + model.getIp());
                    }
                }
            }
            return null;
        });
    }

    public @NonNull CompletableFuture<Void> saveIP(@NonNull IP ip) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            for (StorageService service : cachedConfig.getStorage()) {
                IPModel model = service.getOrCreateIpModel(ip.getIP().getHostAddress(), ip.getType().ordinal());
                model.setCascade(ip.getCascade());
                model.setConsensus(ip.getConsensus());
                service.storeModel(model);
            }

            IPPacket packet = new IPPacket();
            packet.setIp(ip.getIP().getHostAddress());
            packet.setType(ip.getType().ordinal());
            packet.setCascade(ip.getCascade());
            packet.setConsensus(ip.getConsensus());
            PacketUtil.queuePacket(packet);
        });
    }

    public @NonNull CompletableFuture<Void> deleteIP(@NonNull String ip) {
        return CompletableFuture.runAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            for (StorageService service : cachedConfig.getStorage()) {
                IPModel model = new IPModel();
                model.setIp(ip);

                service.deleteModel(model);
            }

            DeleteIPPacket packet = new DeleteIPPacket();
            packet.setIp(ip);
            PacketUtil.queuePacket(packet);
        });
    }

    public @NonNull CompletableFuture<Set<InetAddress>> getIPs() {
        return CompletableFuture.supplyAsync(() -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
            if (cachedConfig == null) {
                throw new APIException(false, "Cached config could not be fetched.");
            }

            Set<InetAddress> retVal = new HashSet<>();
            for (StorageService service : cachedConfig.getStorage()) {
                Set<IPModel> models = service.getAllIps(cachedConfig.getSourceCacheTime());
                if (!models.isEmpty()) {
                    for (IPModel model : models) {
                        try {
                            retVal.add(InetAddress.getByName(model.getIp()));
                        } catch (UnknownHostException ex) {
                            logger.warn("Could not create InetAddress for " + model.getIp());
                        }
                    }
                    break;
                }
            }
            return retVal;
        });
    }

    public @NonNull AlgorithmMethod getCurrentAlgorithmMethod() throws APIException {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            throw new APIException(false, "Cached config could not be fetched.");
        }
        return cachedConfig.getVPNAlgorithmMethod();
    }

    public @NonNull CompletableFuture<Boolean> cascade(@NonNull String ip, boolean useCache) {
        return CompletableFuture.supplyAsync(() -> {
            IPModel model;
            if (useCache) {
                model = ipCache.get(new Pair<>(ip, AlgorithmMethod.CASCADE));
            } else {
                model = calculateIpResult(ip, AlgorithmMethod.CASCADE, false);
            }
            if (model == null) {
                throw new APIException(false, "Could not get data for IP " + ip);
            }
            return model.getCascade();
        });
    }

    public @NonNull CompletableFuture<Double> consensus(@NonNull String ip, boolean useCache) {
        return CompletableFuture.supplyAsync(() -> {
            IPModel model;
            if (useCache) {
                model = ipCache.get(new Pair<>(ip, AlgorithmMethod.CONSESNSUS));
            } else {
                model = calculateIpResult(ip, AlgorithmMethod.CONSESNSUS, false);
            }
            if (model == null) {
                throw new APIException(false, "Could not get data for IP " + ip);
            }
            return model.getConsensus();
        });
    }

    public double getMinConsensusValue() throws APIException {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            throw new APIException(false, "Cached config could not be fetched.");
        }
        return cachedConfig.getVPNAlgorithmConsensus();
    }

    private @NonNull IPModel calculateIpResult(@NonNull String ip, @NonNull AlgorithmMethod method, boolean useCache) throws APIException {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            throw new APIException(false, "Cached config could not be fetched.");
        }

        if (useCache) {
            for (StorageService service : cachedConfig.getStorage()) {
                IPModel model = service.getIpModel(ip, cachedConfig.getSourceCacheTime());
                if (model != null && model.getType() == method.ordinal()) {
                    if (cachedConfig.getDebug()) {
                        logger.info("Found database value for IP " + ip + ".");
                    }
                    return model;
                }
            }
        }

        if (cachedConfig.getDebug()) {
            logger.info("Getting web result for IP " + ip + ".");
        }

        IPModel retVal = new IPModel();
        retVal.setIp(ip);
        retVal.setType(cachedConfig.getVPNAlgorithmMethod().ordinal());

        if (method == AlgorithmMethod.CONSESNSUS) {
            ExecutorService pool = Executors.newWorkStealingPool(cachedConfig.getThreads());
            List<Source<? extends SourceModel>> sources = sourceManager.getSources();
            CountDownLatch latch = new CountDownLatch(sources.size());
            AtomicLong results = new AtomicLong(0L);
            AtomicLong totalSources = new AtomicLong(0L);
            for (Source<? extends SourceModel> source : sources) {
                pool.submit(() -> {
                    if (Boolean.TRUE.equals(sourceInvalidationCache.get(source.getName()))) {
                        if (cachedConfig.getDebug()) {
                            logger.info("Skipping source " + source.getName() + " due to recent failure.");
                        }
                        latch.countDown();
                        return;
                    }
                    if (cachedConfig.getDebug()) {
                        logger.info("Getting result from source " + source.getName() + ".");
                    }
                    try {
                        Boolean result = source.getResult(ip)
                                .exceptionally(this::handleException)
                                .join();
                        if (result != null) {
                            if (Boolean.TRUE.equals(result)) {
                                results.addAndGet(1L);
                            }
                            totalSources.addAndGet(1L);
                        } else {
                            logger.error("Source " + source.getName() + " returned an error. Skipping.");
                            sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                        }
                    } catch (CompletionException ignored) {
                        logger.error("Source " + source.getName() + " returned an error. Skipping.");
                        sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                    } catch (Exception ex) {
                        if (cachedConfig.getDebug()) {
                            logger.error(ex.getMessage(), ex);
                        } else {
                            logger.error(ex.getMessage());
                        }
                        logger.error("Source " + source.getName() + " returned an error. Skipping.");
                        sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                    }
                    latch.countDown();
                });
            }

            try {
                if (!latch.await(20L, TimeUnit.SECONDS)) {
                    logger.warn("Consensus timed out before all sources could be queried.");
                }
            } catch (InterruptedException ex) {
                if (cachedConfig.getDebug()) {
                    logger.error(ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
                Thread.currentThread().interrupt();
            }
            pool.shutdownNow(); // Kill it with fire

            if (totalSources.get() > 0L) {
                retVal.setConsensus((double) results.get() / (double) totalSources.get());
                if (useCache) {
                    storeResult(retVal, cachedConfig);
                    sendResult(retVal, cachedConfig);
                }
                return retVal;
            }
        } else {
            for (Source<? extends SourceModel> source : sourceManager.getSources()) {
                if (Boolean.TRUE.equals(sourceInvalidationCache.get(source.getName()))) {
                    if (cachedConfig.getDebug()) {
                        logger.info("Skipping source " + source.getName() + " due to recent failure.");
                    }
                    continue;
                }
                if (cachedConfig.getDebug()) {
                    logger.info("Getting result from source " + source.getName() + ".");
                }
                try {
                    retVal.setCascade(source.getResult(ip)
                            .exceptionally(this::handleException)
                            .join());
                    if (retVal.getCascade() != null) {
                        break;
                    } else {
                        logger.error("Source " + source.getName() + " returned an error. Skipping.");
                        sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                    }
                } catch (CompletionException ignored) {
                    logger.error("Source " + source.getName() + " returned an error. Skipping.");
                    sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                } catch (Exception ex) {
                    if (cachedConfig.getDebug()) {
                        logger.error(ex.getMessage(), ex);
                    } else {
                        logger.error(ex.getMessage());
                    }
                    logger.error("Source " + source.getName() + " returned an error. Skipping.");
                    sourceInvalidationCache.put(source.getName(), Boolean.TRUE);
                }
            }

            if (useCache && retVal.getCascade() != null) {
                storeResult(retVal, cachedConfig);
                sendResult(retVal, cachedConfig);
            }
            return retVal;
        }

        throw new APIException(false, "No sources were available to query. See https://github.com/egg82/AntiVPN/wiki/FAQ#Errors");
    }

    protected final <T> T handleException(Throwable ex) {
        Throwable oldEx = null;
        if (ex instanceof CompletionException) {
            oldEx = ex;
            ex = ex.getCause();
        }

        if (ex instanceof APIException) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage(), oldEx != null ? oldEx : ex);
            } else {
                logger.error("[Hard: " + ((APIException) ex).isHard() + "] " + ex.getMessage());
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.error(ex.getMessage(), oldEx != null ? oldEx : ex);
            } else {
                logger.error(ex.getMessage());
            }
        }
        return null;
    }

    private void storeResult(@NonNull IPModel model, @NonNull CachedConfig cachedConfig) {
        for (StorageService service : cachedConfig.getStorage()) {
            IPModel m = service.getOrCreateIpModel(model.getIp(), model.getType());
            m.setCascade(model.getCascade());
            m.setConsensus(model.getConsensus());
            service.storeModel(m);
        }

        if (cachedConfig.getDebug()) {
            logger.info("Stored data for " + model.getIp() + " in storage.");
        }
    }

    private void sendResult(@NonNull IPModel model, @NonNull CachedConfig cachedConfig) {
        IPPacket packet = new IPPacket();
        packet.setIp(model.getIp());
        packet.setType(model.getType());
        packet.setCascade(model.getCascade());
        packet.setConsensus(model.getConsensus());
        PacketUtil.queuePacket(packet);

        if (cachedConfig.getDebug()) {
            logger.info("Queued packet for " + model.getIp() + " in messaging.");
        }
    }
}
