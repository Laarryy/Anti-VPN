package me.egg82.antivpn.hooks;

import co.aikar.commands.CommandIssuer;
import me.egg82.antivpn.config.ConfigUtil;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LuckPermsHook implements PluginHook {
    private final CommandIssuer console;

    public LuckPermsHook(@NotNull CommandIssuer console) {
        this.console = console;
    }

    @Override
    public void cancel() { }

    public boolean isExpensive(@NotNull UUID uuid) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        return userManager.isLoaded(uuid) && userManager.getUser(uuid) != null;
    }

    public @NotNull CompletableFuture<UUID> getUuid(@NotNull String name) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        return userManager.lookupUniqueId(name);
    }

    public @NotNull CompletableFuture<Boolean> hasPermission(@NotNull UUID uuid, @NotNull String permission) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        ContextManager contextManager = LuckPermsProvider.get().getContextManager();

        if (ConfigUtil.getDebugOrFalse()) {
            if (userManager.isLoaded(uuid) && userManager.getUser(uuid) != null) {
                console.sendMessage("<c2>LuckPerms UUID</c2> <c1>" + uuid + "</c1><c2> is previously loaded, using cached data..</c2>");
            } else {
                console.sendMessage("<c2>LuckPerms UUID</c2> <c1>" + uuid + "</c1><c2> is not loaded, forcing data load..</c2>");
            }
        }

        return userManager.loadUser(uuid).thenApply(user -> {
            ImmutableContextSet contexts = contextManager.getContext(user).orElseGet(contextManager::getStaticContext);
            return user.getCachedData().getPermissionData(QueryOptions.contextual(contexts)).checkPermission(permission).asBoolean();
        });
    }
}

