package me.egg82.antivpn.hooks;

import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ExtensionService;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.annotation.BooleanProvider;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import ninja.egg82.events.BukkitEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = new GELFLogger(LoggerFactory.getLogger(getClass()));
    private final CapabilityService capabilities;

    public static void create(@NotNull Plugin plugin, @NotNull Plugin plan) {
        if (!plan.isEnabled()) {
            BukkitEvents.subscribe(plugin, PluginEnableEvent.class, EventPriority.MONITOR)
                    .expireIf(e -> e.getPlugin().getName().equals("Plan"))
                    .filter(e -> e.getPlugin().getName().equals("Plan"))
                    .handler(e -> hook = new PlayerAnalyticsHook());
            return;
        }
        hook = new PlayerAnalyticsHook();
    }

    private static PlayerAnalyticsHook hook = null;

    public @Nullable PlayerAnalyticsHook get() { return hook; }

    private PlayerAnalyticsHook() {
        capabilities = CapabilityService.getInstance();

        if (isCapabilityAvailable("DATA_EXTENSION_VALUES") && isCapabilityAvailable("DATA_EXTENSION_TABLES")) {
            try {
                ExtensionService.getInstance().register(new Data());
                capabilities.registerEnableListener(enabled -> {
                    if (Boolean.TRUE.equals(enabled)) {
                        ExtensionService.getInstance().register(new Data());
                    }
                });
            } catch (NoClassDefFoundError ex) {
                // Plan not installed
                logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.HOOK__PLAN__NOT_INSTALLED), ex);
            } catch (IllegalStateException ex) {
                // Plan not enabled
                logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.HOOK__PLAN__NOT_ENABLED), ex);
            } catch (IllegalArgumentException ex) {
                // DataExtension impl error
                logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.HOOK__PLAN__DATA_EXCEPTION), ex);
            }
        }

        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() { }

    private boolean isCapabilityAvailable(@NotNull String capability) {
        try {
            return capabilities.hasCapability(capability);
        } catch (NoClassDefFoundError ignored) {
            return false;
        }
    }

    @PluginInfo(
            name = "Anti-VPN",
            iconName = "shield-alt",
            iconFamily = Family.SOLID,
            color = Color.BLUE
    )
    class Data implements DataExtension {
        private final CallEvents[] events = new CallEvents[] { CallEvents.SERVER_PERIODICAL, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.PLAYER_JOIN };

        private Data() { }

        @NumberProvider(
                text = "VPN Users",
                description = "Number of online VPN users.",
                priority = 2,
                iconName = "user-shield",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE,
                format = FormatType.NONE
        )
        public long getVpns() {
            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            ExecutorService pool = Executors.newWorkStealingPool(Math.max(players.size() / 2, Runtime.getRuntime().availableProcessors() / 2));
            CountDownLatch latch = new CountDownLatch(players.size());
            AtomicLong results = new AtomicLong(0L);

            for (Player p : players) {
                pool.submit(() -> {
                    String ip = getIp(p);
                    if (ip == null || ip.isEmpty()) {
                        latch.countDown();
                        return;
                    }

                    if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                        try {
                            if (ipManager.consensus(ip, true).get() >= ipManager.getMinConsensusValue()) {
                                results.addAndGet(1L);
                            }
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException | CancellationException ex) {
                            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        }
                    } else {
                        try {
                            if (ipManager.cascade(ip, true).get()) {
                                results.addAndGet(1L);
                            }
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException | CancellationException ex) {
                            logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                        }
                    }
                    latch.countDown();
                });
            }

            try {
                if (!latch.await(40L, TimeUnit.SECONDS)) {
                    logger.warn(LocaleUtil.getDefaultI18N().getText(MessageKey.HOOK__PLAN__TIMEOUT));
                }
            } catch (InterruptedException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }
            pool.shutdownNow(); // Kill it with fire

            return results.get();
        }

        @NumberProvider(
                text = "MCLeaks Users",
                description = "Number of online MCLeaks users.",
                priority = 1,
                iconName = "users",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE,
                format = FormatType.NONE
        )
        public long getMcLeaks() {
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            ExecutorService pool = Executors.newWorkStealingPool(Math.max(players.size() / 2, Runtime.getRuntime().availableProcessors() / 2));
            CountDownLatch latch = new CountDownLatch(players.size());
            AtomicLong results = new AtomicLong(0L);

            for (Player p : players) {
                pool.submit(() -> {
                    try {
                        if (playerManager.checkMcLeaks(p.getUniqueId(), true).get()) {
                            results.addAndGet(1L);
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException | CancellationException ex) {
                        logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                    }
                    latch.countDown();
                });
            }

            try {
                if (!latch.await(40L, TimeUnit.SECONDS)) {
                    logger.warn(LocaleUtil.getDefaultI18N().getText(MessageKey.HOOK__PLAN__TIMEOUT));
                }
            } catch (InterruptedException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                Thread.currentThread().interrupt();
            }
            pool.shutdownNow(); // Kill it with fire

            return results.get();
        }

        @BooleanProvider(
                text = "VPN",
                description = "Using a VPN or proxy.",
                iconName = "user-shield",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE
        )
        public boolean getUsingVpn(@NotNull UUID playerID) {
            Player player = Bukkit.getPlayer(playerID);
            if (player == null) {
                return false;
            }

            String ip = getIp(player);
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

            if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    return ipManager.consensus(ip, true).get() >= ipManager.getMinConsensusValue();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            } else {
                try {
                    return ipManager.cascade(ip, true).get();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                }
            }

            return false;
        }

        @BooleanProvider(
                text = "MCLeaks",
                description = "Using an MCLeaks account.",
                iconName = "users",
                iconFamily = Family.SOLID,
                iconColor = Color.NONE
        )
        public boolean getMcLeaks(@NotNull UUID playerId) {
            PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

            try {
                return playerManager.checkMcLeaks(playerId, true).get();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
            }

            return false;
        }

        private @Nullable String getIp(@NotNull Player player) {
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                return null;
            }
            InetAddress host = address.getAddress();
            if (host == null) {
                return null;
            }
            return host.getHostAddress();
        }

        @Override
        public @NotNull CallEvents[] callExtensionMethodsOn() { return events; }
    }
}
