package me.egg82.antivpn.hooks;

import net.milkbowl.vault.permission.Permission;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class VaultHook implements PluginHook {

    public final Logger logger = LoggerFactory.getLogger(getClass());
    public Permission permission;

    public static void create(Plugin plugin, Plugin vault) {
        if (vault != null && !vault.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("Vault"))
                    .filter(e -> e.getPlugin().getName().equals("Vault"))
                    .handler(e -> ServiceLocator.register(new VaultHook()));
            return;
        }
        ServiceLocator.register(new VaultHook());
    }



    public VaultHook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") != null || Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            final RegisteredServiceProvider<Permission> permissionProvider =
                    Bukkit.getServicesManager().getRegistration(Permission.class);
            if (permissionProvider != null) {
                logger.info("Found Permissions Provider");
                permission = permissionProvider.getProvider();
            } else {
                logger.info("Vault permissions not detected.");
                permission = null;
            }
        } else {
            logger.info("Vault was not found.");
            permission = null;
        }
    }

    @Override
    public void cancel() {
    }
}



