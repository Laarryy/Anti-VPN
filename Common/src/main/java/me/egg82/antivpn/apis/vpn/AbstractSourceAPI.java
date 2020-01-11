package me.egg82.antivpn.apis.vpn;

import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.apis.SourceAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import ninja.leaping.configurate.ConfigurationNode;

public abstract class AbstractSourceAPI implements SourceAPI {
    protected final ConfigurationNode getSourceConfigNode() throws APIException {
        Optional<Configuration> config = ConfigUtil.getConfig();
        if (!config.isPresent()) {
            throw new APIException(true, "Could not get configuration.");
        }

        return config.get().getNode("sources", getName());
    }

    protected final CachedConfigValues getCachedConfig() throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Cached config could not be fetched.");
        }

        return cachedConfig.get();
    }
}
