package me.egg82.antivpn.events;

import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.hooks.VaultHook;
import net.milkbowl.vault.permission.Permission;
import ninja.egg82.events.BukkitEventSubscriber;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class EventHolder {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final boolean isVaultEnabled = Bukkit.getPluginManager().isPluginEnabled("Vault");

    protected final List<BukkitEventSubscriber<?>> events = new ArrayList<>();

    protected final VPNAPI api = VPNAPI.getInstance();

    public final VaultHook vaultHook = new VaultHook(Bukkit.getPluginManager().getPlugin("Vault"));

    public final int numEvents() { return events.size(); }

    public final void cancel() {
        for (BukkitEventSubscriber<?> event : events) {
            event.cancel();
        }
    }
}
