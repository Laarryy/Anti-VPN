package me.egg82.avpn;

import com.google.common.collect.ImmutableSet;

public class Config {
    // vars
    public static volatile ImmutableSet<String> sources = null;
    public static volatile long sourceCacheTime = -1L;

    public static volatile ImmutableSet<String> ignore = null;

    public static volatile boolean debug = false;
    public static volatile String kickMessage = null;
    public static volatile boolean async = true;
    public static volatile boolean kick = true;
    public static volatile double consensus = -1.0d;

    public static volatile boolean sendUsage = true;
    public static volatile boolean sendErrors = true;

    public static volatile boolean checkUpdates = true;
    public static volatile boolean notifyUpdates = true;

    public static final String ROLLBAR_KEY = "dccf7919d6204dfea740702ad41ee08c";
    public static final String GAMEANALYTICS_KEY = "10b55aa4f41d64ff258f9c66a5fbf9ec";
    public static final String GAMEANALYTICS_SECRET = "3794acfebab1122e852d73bbf505c37f42bf3f41";

    // constructor
    public Config() {

    }

    // public

    // private

}
