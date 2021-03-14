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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.AlgorithmMethod;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerAnalyticsHook implements PluginHook {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CapabilityService capabilities;

    public PlayerAnalyticsHook(@NotNull ProxyServer proxy) {
        capabilities = CapabilityService.getInstance();

        if (isCapabilityAvailable("DATA_EXTENSION_VALUES") && isCapabilityAvailable("DATA_EXTENSION_TABLES")) {
            try {
                ExtensionService.getInstance().register(new Data(proxy));
            } catch (NoClassDefFoundError ex) {
                // Plan not installed
                logger.error("Plan is not installed.", ex);
            } catch (IllegalStateException ex) {
                // Plan not enabled
                logger.error("Plan is not enabled.", ex);
            } catch (IllegalArgumentException ex) {
                // DataExtension impl error
                logger.error("DataExtension implementation exception.", ex);
            }
        }
    }

    @Override
    public void cancel() { }

    private boolean isCapabilityAvailable(String capability) {
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
        private final ProxyServer proxy;
        private final CallEvents[] events = new CallEvents[] { CallEvents.SERVER_PERIODICAL, CallEvents.SERVER_EXTENSION_REGISTER, CallEvents.PLAYER_JOIN };

        private Data(@NotNull ProxyServer proxy) {
            this.proxy = proxy;
        }

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

            Collection<Player> players = proxy.getAllPlayers();
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
                            Double val = ipManager.consensus(ip, true).get();
                            if (val != null && val >= ipManager.getMinConsensusValue()) {
                                results.addAndGet(1L);
                            }
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException | CancellationException ex) {
                            ExceptionUtil.handleException(ex, logger);
                        }
                    } else {
                        try {
                            if (Boolean.TRUE.equals(ipManager.cascade(ip, true).get())) {
                                results.addAndGet(1L);
                            }
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        } catch (ExecutionException | CancellationException ex) {
                            ExceptionUtil.handleException(ex, logger);
                        }
                    }
                    latch.countDown();
                });
            }

            try {
                if (!latch.await(40L, TimeUnit.SECONDS)) {
                    logger.warn("Plan hook timed out before all results could be obtained.");
                }
            } catch (InterruptedException ex) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
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

            Collection<Player> players = proxy.getAllPlayers();
            ExecutorService pool = Executors.newWorkStealingPool(Math.max(players.size() / 2, Runtime.getRuntime().availableProcessors() / 2));
            CountDownLatch latch = new CountDownLatch(players.size());
            AtomicLong results = new AtomicLong(0L);

            for (Player p : players) {
                pool.submit(() -> {
                    try {
                        if (Boolean.TRUE.equals(playerManager.checkMcLeaks(p.getUniqueId(), true).get())) {
                            results.addAndGet(1L);
                        }
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException | CancellationException ex) {
                        ExceptionUtil.handleException(ex, logger);
                    }
                    latch.countDown();
                });
            }

            try {
                if (!latch.await(40L, TimeUnit.SECONDS)) {
                    logger.warn("Plan hook timed out before all results could be obtained.");
                }
            } catch (InterruptedException ex) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.error(ex.getClass().getName() + ": " + ex.getMessage(), ex);
                } else {
                    logger.error(ex.getMessage());
                }
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
            Optional<Player> player = proxy.getPlayer(playerID);
            if (!player.isPresent()) {
                return false;
            }

            String ip = getIp(player.get());
            if (ip == null || ip.isEmpty()) {
                return false;
            }

            IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

            if (ipManager.getCurrentAlgorithmMethod() == AlgorithmMethod.CONSESNSUS) {
                try {
                    Double val = ipManager.consensus(ip, true).get();
                    return val != null && val >= ipManager.getMinConsensusValue();
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
                }
            } else {
                try {
                    return Boolean.TRUE.equals(ipManager.cascade(ip, true).get());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException | CancellationException ex) {
                    ExceptionUtil.handleException(ex, logger);
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
                return Boolean.TRUE.equals(playerManager.checkMcLeaks(playerId, true).get());
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException | CancellationException ex) {
                ExceptionUtil.handleException(ex, logger);
            }

            return false;
        }

        private @Nullable String getIp(@NotNull Player player) {
            InetSocketAddress address = player.getRemoteAddress();
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
