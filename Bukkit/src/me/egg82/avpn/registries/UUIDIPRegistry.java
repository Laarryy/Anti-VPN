package me.egg82.avpn.registries;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import ninja.egg82.patterns.registries.ExpiringRegistry;

public class UUIDIPRegistry extends ExpiringRegistry<UUID, String> {
    // vars

    // constructor
    public UUIDIPRegistry() {
        super(UUID.class, String.class, 60L * 1000L, TimeUnit.MILLISECONDS);
    }

    // public

    // private

}
