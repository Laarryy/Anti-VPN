package me.egg82.antivpn.api;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Provides static access to the {@link VPNAPI} service.
 */
public final class VPNAPIProvider {
    private VPNAPIProvider() { }

    private static VPNAPI instance = null;

    /**
     * Return an instance of the {@link VPNAPI} service.
     *
     * @return The current {@link VPNAPI} service
     * @throws IllegalStateException if not yet loaded
     */
    public static @NonNull VPNAPI getInstance() {
        VPNAPI i = instance;
        if (i == null) {
            throw new IllegalStateException("VPNAPI is not loaded.");
        }
        return i;
    }

    private static void register(@NonNull VPNAPI instance) { VPNAPIProvider.instance = instance; }

    private static void deregister() { VPNAPIProvider.instance = null; }
}
