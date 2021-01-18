package me.egg82.antivpn.hooks;

import co.aikar.commands.CommandIssuer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.config.ConfigUtil;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import org.checkerframework.checker.nullness.qual.NonNull;

public class LuckPermsHook implements PluginHook {
    private final CommandIssuer console;

    public LuckPermsHook(@NonNull CommandIssuer console) {
        this.console = console;
    }

    public void cancel() { }

    public boolean isExpensive(@NonNull UUID uuid) {
        UserManager userManager = LuckPermsProvider.get().getUserManager();
        return userManager.isLoaded(uuid) && userManager.getUser(uuid) != null;
    }

    public @NonNull CompletableFuture<Boolean> hasPermission(@NonNull UUID uuid, @NonNull String permission) {
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

