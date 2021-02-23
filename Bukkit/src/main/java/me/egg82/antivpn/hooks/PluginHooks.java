package me.egg82.antivpn.hooks;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

public class PluginHooks {
    private static final List<PluginHook> hooks = new CopyOnWriteArrayList<>();
    public static @NotNull List<PluginHook> getHooks() { return hooks; }

    private PluginHooks() { }
}
