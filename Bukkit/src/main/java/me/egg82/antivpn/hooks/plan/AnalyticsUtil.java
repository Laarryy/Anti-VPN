package me.egg82.antivpn.hooks.plan;

import java.util.concurrent.atomic.AtomicLong;

public class AnalyticsUtil {
    private AnalyticsUtil() { }

    private static final AtomicLong blockedVPNs = new AtomicLong(0L);

    private static final AtomicLong blockedMCLeaks = new AtomicLong(0L);

    public static void incrementBlockedVPNs() { blockedVPNs.getAndIncrement(); }

    public static void incrementBlockedMCLeaks() { blockedMCLeaks.getAndIncrement(); }

    public static long getBlockedVPNs() { return blockedVPNs.getAndSet(0L); }

    public static long getBlockedMCLeaks() { return blockedMCLeaks.getAndSet(0L); }
}
