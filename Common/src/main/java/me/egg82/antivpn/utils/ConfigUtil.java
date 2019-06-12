package me.egg82.antivpn.utils;

import java.util.Optional;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import ninja.egg82.service.ServiceLocator;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    private ConfigUtil() {}

    /**
     * Grabs the config instance from ServiceLocator
     * @return Optional, instance of the Configuration class
     */
    public static Optional<Configuration> getConfig() {
        try {
            return ServiceLocator.getOptional(Configuration.class);
        } catch (IllegalAccessException | InstantiationException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.empty();
    }

    /**
     * Grabs the cached config instance from ServiceLocator
     * @return Optional, instance of the CachedConfigValues class
     */
    public static Optional<CachedConfigValues> getCachedConfig() {
        try {
            return ServiceLocator.getOptional(CachedConfigValues.class);
        } catch (IllegalAccessException | InstantiationException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return Optional.empty();
    }

    public static ConfigurationNode getStorageNodeOrNull() {
        Optional<Configuration> config = getConfig();
        return (config.isPresent()) ? config.get().getNode("storage") : null;
    }

    public static ConfigurationNode getRedisNodeOrNull() {
        Optional<Configuration> config = getConfig();
        return (config.isPresent()) ? config.get().getNode("redis") : null;
    }

    public static boolean getDebugOrFalse() {
        Optional<CachedConfigValues> cachedConfig = getCachedConfig();
        return cachedConfig.isPresent() && cachedConfig.get().getDebug();
    }
}
