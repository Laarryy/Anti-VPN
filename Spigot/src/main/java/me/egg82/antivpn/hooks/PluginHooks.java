package me.egg82.antivpn.hooks;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PluginHooks {
    private static final List<PluginHook> hooks = new CopyOnWriteArrayList<>();

    public static @NotNull List<PluginHook> getHooks() { return hooks; }

    private PluginHooks() { }
}
