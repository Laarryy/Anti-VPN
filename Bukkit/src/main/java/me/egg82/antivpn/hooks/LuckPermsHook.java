package me.egg82.antivpn.hooks;

import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.MessageKey;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsHook implements PluginHook {
    public static void create(@NotNull Plugin plugin, @NotNull Plugin luckperms) {
        if (!luckperms.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .filter(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .handler(e -> hook = new LuckPermsHook());
            return;
        }
        hook = new LuckPermsHook();
    }

    private static LuckPermsHook hook = null;

    public static @Nullable LuckPermsHook get() { return hook; }

    private LuckPermsHook() {
        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() { }

    public boolean isExpensive(@NotNull UUID uuid) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        return userManager.isLoaded(uuid) && userManager.getUser(uuid) != null;
    }

    public @NotNull CompletableFuture<@NotNull UUID> getUuid(@NotNull String name) { return LuckPermsProvider.get().getUserManager().lookupUniqueId(name); }

    public @NotNull CompletableFuture<@NotNull Boolean> hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        ContextManager contextManager = LuckPermsProvider.get().getContextManager();

        if (ConfigUtil.getDebugOrFalse()) {
            if (userManager.isLoaded(uuid) && userManager.getUser(uuid) != null) {
                BukkitLocaleCommandUtil.getConsole().sendMessage(MessageKey.HOOK__LUCKPERMS__CACHED, "{uuid}", uuid.toString());
            } else {
                BukkitLocaleCommandUtil.getConsole().sendMessage(MessageKey.HOOK__LUCKPERMS__UNCACHED, "{uuid}", uuid.toString());
            }
        }

        return userManager.loadUser(uuid).thenApply(user -> {
            ImmutableContextSet contexts = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
            return user.getCachedData().getPermissionData(QueryOptions.contextual(contexts)).checkPermission(permission).asBoolean();
        });
    }
}

