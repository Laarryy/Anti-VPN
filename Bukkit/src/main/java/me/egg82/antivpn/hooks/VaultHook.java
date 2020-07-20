package me.egg82.antivpn.hooks;

import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.milkbowl.vault.permission.Permission;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultHook implements PluginHook {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Permission permission;

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

    private VaultHook() {
        final RegisteredServiceProvider<Permission> permissionProvider =
                Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permissionProvider != null) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + " Found permissions provider");
            }
            permission = permissionProvider.getProvider();
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + " Vault permissions not detected.");
            }
            permission = null;
        }
    }

    @Override
    public void cancel() {
    }
    /* can return null */
    public Permission getPermission() {
            if (permission == null && ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + " Permission is null");
            }
            return this.permission;
        }
    }

