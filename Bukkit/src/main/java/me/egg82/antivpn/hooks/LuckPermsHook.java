package me.egg82.antivpn.hooks;

import co.aikar.commands.CommandIssuer;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.ExceptionUtil;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckPermsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CommandIssuer console;

    public static void create(@NonNull Plugin plugin, @NonNull Plugin luckperms, @NonNull CommandIssuer console) {
        if (!luckperms.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .filter(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .handler(e -> ServiceLocator.register(new LuckPermsHook(console)));
            return;
        }
        ServiceLocator.register(new LuckPermsHook(console));
    }

    private LuckPermsHook(@NonNull CommandIssuer console) {
        this.console = console;
    }

    public void cancel() { }

    public boolean isExpensive(@NonNull UUID uuid) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        return !userManager.isLoaded(uuid) && userManager.getUser(uuid) != null;
    }

    /*
    Note: Can be an expensive operation
     */
    public boolean hasPermission(@NonNull UUID uuid, @NonNull String permission) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        ContextManager contextManager = LuckPermsProvider.get().getContextManager();

        User user = null;
        if (userManager.isLoaded(uuid)) {
            user = userManager.getUser(uuid);
        }
        if (user == null) {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage("<c2>UUID</c2> <c1>" + uuid + "</c1><c2> is not loaded, forcing data load..</c2>");
            }
            CompletableFuture<User> future = userManager.loadUser(uuid);
            try {
                user = future.get(); // Expensive
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
                return false;
            }
            if (user == null) {
                return false;
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                console.sendMessage("<c2>UUID</c2> <c1>" + uuid + "</c1><c2> is previously loaded, using cached data..</c2>");
            }
        }

        ImmutableContextSet contexts = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
        return user.getCachedData().getPermissionData(QueryOptions.contextual(contexts)).checkPermission(permission).asBoolean();
    }
}

