package me.egg82.antivpn.hooks;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.core.Pair;
import me.egg82.antivpn.locale.BukkitLocaleCommandUtil;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.update.BukkitUpdater;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UpdaterHook implements PluginHook {
    private final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));

    public static void create(@NotNull Plugin plugin, int id) {
        hook = new UpdaterHook(new BukkitUpdater(plugin, id));
    }

    private static UpdaterHook hook = null;

    public static @Nullable UpdaterHook get() { return hook; }

    private final ScheduledExecutorService workPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Anti-VPN_Updater_%d").build());
    private final BukkitUpdater updater;

    private UpdaterHook(@NotNull BukkitUpdater updater) {
        this.updater = updater;

        workPool.scheduleWithFixedDelay(this::checkUpdate, 1L, 61L, TimeUnit.MINUTES);

        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(2L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public void checkUpdate(@NotNull BukkitLocalizedCommandSender commandSender) {
        ConfigurationNode config = ConfigUtil.getConfig();

        if (!config.node("update", "check").getBoolean(true)) {
            return;
        }

        updater.isUpdateAvailable()
                .thenCombineAsync(updater.getLatestVersion(), Pair::new)
                .whenCompleteAsync((val, ex) -> {
                    if (ex != null) {
                        logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        return;
                    }

                    if (!Boolean.TRUE.equals(val.getT1())) {
                        return;
                    }

                    if (commandSender.isConsole() || config.node("update", "notify").getBoolean(true)) {
                        commandSender.sendMessage(MessageKey.GENERAL__UPDATE_FOUND, "{version}", val.getT2());
                    }
                });
    }

    private void checkUpdate() {
        checkUpdate(BukkitLocaleCommandUtil.getConsole());
    }
}
