package me.egg82.avpn.registries;

import java.util.concurrent.TimeUnit;

import me.egg82.avpn.Configuration;
import ninja.egg82.enums.ExpirationPolicy;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.ExpiringRegistry;
import ninja.egg82.utils.TimeUtil;

public class ConsensusRegistry extends ExpiringRegistry<String, Double> {
    // vars

    // constructor
    public ConsensusRegistry() {
        super(String.class, Double.class, TimeUtil.getTime(ServiceLocator.getService(Configuration.class).getNode("cacheTime").getString("1minute")), TimeUnit.MILLISECONDS, ExpirationPolicy.ACCESSED);
    }

    // public

    // private

}
