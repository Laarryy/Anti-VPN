package me.egg82.antivpn.apis.vpn;

import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.apis.VPNAPI;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.extended.Configuration;
import me.egg82.antivpn.utils.ConfigUtil;
import ninja.leaping.configurate.ConfigurationNode;

public abstract class AbstractVPNAPI implements VPNAPI {
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
            throw new APIException(true, "Could not get cached configuration.");
        }

        return cachedConfig.get();
    }
}
