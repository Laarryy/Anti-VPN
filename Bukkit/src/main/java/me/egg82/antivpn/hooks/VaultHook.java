package me.egg82.antivpn.hooks;

import co.aikar.commands.CommandIssuer;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.milkbowl.vault.permission.Permission;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VaultHook implements PluginHook {
    private final Permission permission;
    private final CommandIssuer console;

    public static void create(@NonNull Plugin plugin, @NonNull Plugin vault, @NonNull CommandIssuer console) {
        if (!vault.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("Vault"))
                    .filter(e -> e.getPlugin().getName().equals("Vault"))
                    .handler(e -> ServiceLocator.register(new VaultHook(console)));
            return;
        }
        ServiceLocator.register(new VaultHook(console));
    }

    private VaultHook(@NonNull CommandIssuer console) {
        this.console = console;
        final RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage(LogUtil.HEADING + "<c2>Found Vault permissions provider.</c2>");
            }
            permission = permissionProvider.getProvider();
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage(LogUtil.HEADING + "<c9>Could not find Vault permissions provider.</c9>");
            }
            permission = null;
        }
    }

    public void cancel() { }

    public @Nullable Permission getPermission() {
        if (permission == null && ConfigUtil.getDebugOrFalse()) {
            console.sendMessage(LogUtil.HEADING + "<c2>Returning null Vault permissions provider.</c2>");
        }
        return this.permission;
    }
}

