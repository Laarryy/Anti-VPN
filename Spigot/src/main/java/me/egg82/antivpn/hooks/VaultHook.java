package me.egg82.antivpn.hooks;

import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import me.egg82.antivpn.locale.MessageKey;
import net.milkbowl.vault.permission.Permission;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultHook implements PluginHook {
    private final Permission permission;

    public static void create(@NotNull Plugin plugin, @NotNull Plugin vault) {
        if (!vault.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("Vault"))
                    .filter(e -> e.getPlugin().getName().equals("Vault"))
                    .handler(e -> hook = new VaultHook());
            return;
        }
        hook = new VaultHook();
    }

    private static VaultHook hook = null;

    public static @Nullable VaultHook get() { return hook; }

    private VaultHook() {
        BukkitLocalizedCommandSender console = BukkitLocaleCommandUtil.getConsole();
        RegisteredServiceProvider<Permission> permissionProvider = Bukkit.getServicesManager().getRegistration(Permission.class);

        if (permissionProvider != null) {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage(MessageKey.HOOK__VAULT__FOUND);
            }
            permission = permissionProvider.getProvider();
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage(MessageKey.HOOK__VAULT__NOT_FOUND);
            }
            permission = null;
        }

        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() { }

    public @Nullable Permission getPermission() {
        if (permission == null && ConfigUtil.getDebugOrFalse()) {
            BukkitLocaleCommandUtil.getConsole().sendMessage(MessageKey.HOOK__VAULT__NULL_PROVIDER);
        }
        return this.permission;
    }
}

