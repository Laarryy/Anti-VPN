package me.egg82.antivpn.hooks;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.LogUtil;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import ninja.egg82.events.BukkitEvents;
import ninja.egg82.service.ServiceLocator;
import org.bukkit.ChatColor;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuckPermsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final LuckPerms api;
    private final UserManager userManager;
    private final ContextManager contextManager;

    public static void create(Plugin plugin, Plugin luckperms) {
        if (luckperms != null && !luckperms.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .filter(e -> e.getPlugin().getName().equals("LuckPerms"))
                    .handler(e -> ServiceLocator.register(new LuckPermsHook()));
            return;
        }
        ServiceLocator.register(new LuckPermsHook());
    }

    private LuckPermsHook() {
        api = LuckPermsProvider.get();
        userManager = api.getUserManager();
        contextManager = api.getContextManager();
    }

    public void cancel() { }

    public boolean isExpensive(UUID uuid) { return !userManager.isLoaded(uuid) && userManager.getUser(uuid) != null; }

    /*
    Note: Can be an expensive operation
     */
    public boolean hasPermission(UUID uuid, String permission) {
        User user = null;
        if (userManager.isLoaded(uuid)) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "UUID " + ChatColor.WHITE + uuid.toString() + ChatColor.YELLOW + " is previously loaded, fetching cached data..");
            }
            user = userManager.getUser(uuid);
        }
        if (user == null) {
            if (ConfigUtil.getDebugOrFalse()) {
                logger.info(LogUtil.getHeading() + ChatColor.YELLOW + "UUID " + ChatColor.WHITE + uuid.toString() + ChatColor.YELLOW + " is not loaded, forcing data load..");
            }
            CompletableFuture<User> future = userManager.loadUser(uuid);
            user = future.join(); // Expensive
        }

        ImmutableContextSet contexts = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
        return user.getCachedData().getPermissionData(QueryOptions.contextual(contexts)).checkPermission(permission).asBoolean();
    }
}

