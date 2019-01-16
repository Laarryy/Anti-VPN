package me.egg82.antivpn.services;

import java.util.concurrent.atomic.AtomicLong;

public class AnalyticsHelper {
    private AnalyticsHelper() {}

    private static final AtomicLong blocked = new AtomicLong(0L);

    public static void incrementBlocked() { blocked.getAndIncrement(); }

    public static long getBlocked() { return blocked.get(); }
}
