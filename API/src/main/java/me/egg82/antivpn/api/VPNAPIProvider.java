package me.egg82.antivpn.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static access to the {@link VPNAPI} service.
 */
public final class VPNAPIProvider {
    private VPNAPIProvider() { }

    private static VPNAPI instance = null;

    /**
     * Return an instance of the {@link VPNAPI} service.
     * @return The current {@link VPNAPI} service, or null if not yet loaded
     */
    public static @Nullable VPNAPI getInstance() { return instance; }

    private static void register(@NonNull VPNAPI instance) { VPNAPIProvider.instance = instance; }

    private static void remove() { VPNAPIProvider.instance = null; }
}
