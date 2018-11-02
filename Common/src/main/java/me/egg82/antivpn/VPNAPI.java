package me.egg82.antivpn;

import com.google.common.collect.ImmutableMap;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.services.InternalAPI;
import me.egg82.antivpn.utils.RabbitMQUtil;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.service.ServiceLocator;
import ninja.egg82.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VPNAPI {
    private static final Logger logger = LoggerFactory.getLogger(VPNAPI.class);

    private static VPNAPI api = new VPNAPI();
    private InternalAPI internalApi = new InternalAPI();

    private VPNAPI() {}

    public static VPNAPI getInstance() { return api; }

    public ImmutableMap<String, Optional<Boolean>> test(String ip) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        CachedConfigValues cachedConfig;
        Configuration config;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            config = ServiceLocator.get(Configuration.class);
        } catch (IllegalAccessException | InstantiationException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return ImmutableMap.of();
        }

        return ImmutableMap.copyOf(internalApi.test(ip, cachedConfig.getSources(), config.getNode("sources"), cachedConfig.getDebug()));
    }

    public Optional<Boolean> getResult(String ip, String sourceName) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceName == null) {
            throw new IllegalArgumentException("sourceName cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        Configuration config;

        try {
            config = ServiceLocator.get(Configuration.class);
        } catch (IllegalAccessException | InstantiationException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return Optional.empty();
        }

        return internalApi.getResult(ip, sourceName, config.getNode("sources", sourceName));
    }

    public double consensus(String ip) { return consensus(ip, true); }

    public double consensus(String ip, boolean expensive) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        CachedConfigValues cachedConfig;
        Configuration config;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            config = ServiceLocator.get(Configuration.class);
        } catch (IllegalAccessException | InstantiationException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return 0.0d;
        }

        try (Connection rabbitConnection = RabbitMQUtil.getConnection(cachedConfig.getRabbitConnectionFactory())) {
            return internalApi.consensus(ip, expensive, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"), rabbitConnection, cachedConfig.getSQL(), config.getNode("storage"), cachedConfig.getSQLType(), cachedConfig.getSources(), config.getNode("sources"), cachedConfig.getDebug());
        } catch (IOException | TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return internalApi.consensus(ip, expensive, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"), null, cachedConfig.getSQL(), config.getNode("storage"), cachedConfig.getSQLType(), cachedConfig.getSources(), config.getNode("sources"), cachedConfig.getDebug());
    }

    public boolean isVPN(String ip) { return isVPN(ip, true); }

    public boolean isVPN(String ip, boolean expensive) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        CachedConfigValues cachedConfig;
        Configuration config;

        try {
            cachedConfig = ServiceLocator.get(CachedConfigValues.class);
            config = ServiceLocator.get(Configuration.class);
        } catch (IllegalAccessException | InstantiationException | ServiceNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
            return false;
        }

        try (Connection rabbitConnection = RabbitMQUtil.getConnection(cachedConfig.getRabbitConnectionFactory())) {
            return internalApi.isVPN(ip, expensive, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"), rabbitConnection, cachedConfig.getSQL(), config.getNode("storage"), cachedConfig.getSQLType(), cachedConfig.getSources(), config.getNode("sources"), cachedConfig.getDebug());
        } catch (IOException | TimeoutException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return internalApi.isVPN(ip, expensive, cachedConfig.getSourceCacheTime(), cachedConfig.getRedisPool(), config.getNode("redis"), null, cachedConfig.getSQL(), config.getNode("storage"), cachedConfig.getSQLType(), cachedConfig.getSources(), config.getNode("sources"), cachedConfig.getDebug());
    }
}
