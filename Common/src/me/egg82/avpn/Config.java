package me.egg82.avpn;

import com.google.common.collect.ImmutableSet;

public class Config {
    // vars
    public static volatile ImmutableSet<String> sources = null;
    public static volatile ImmutableSet<String> ignore = null;
    public static volatile long sourceCacheTime = -1L;
    public static volatile boolean debug = false;
    public static volatile String kickMessage = null;
    public static volatile boolean async = true;
    public static volatile boolean kick = true;
    public static volatile double consensus = -1.0d;

    // constructor
    public Config() {

    }

    // public

    // private

}
