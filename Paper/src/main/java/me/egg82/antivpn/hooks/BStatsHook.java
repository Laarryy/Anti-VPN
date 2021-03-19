package me.egg82.antivpn.hooks;

import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.messaging.MessagingService;
import me.egg82.antivpn.storage.StorageService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class BStatsHook implements PluginHook {
    private static final AtomicLong blockedVPNs = new AtomicLong(0L);

    public static void incrementBlockedVPNs() {
        blockedVPNs.getAndIncrement();
    }

    private static final AtomicLong blockedMCLeaks = new AtomicLong(0L);

    public static void incrementBlockedMCLeaks() {
        blockedMCLeaks.getAndIncrement();
    }

    public static void create(@NotNull JavaPlugin plugin, int id) {
        hook = new BStatsHook(plugin, id);
    }

    private static BStatsHook hook = null;

    public static @Nullable BStatsHook getHook() { return hook; }

    private BStatsHook(@NotNull JavaPlugin plugin, int id) {
        if (!ConfigUtil.getConfig().node("stats", "usage").getBoolean(true)) {
            PluginHooks.getHooks().add(this);
            return;
        }

        Metrics metrics = new Metrics(plugin, id);
        metrics.addCustomChart(new SingleLineChart("blocked_vpns", () -> (int) blockedVPNs.getAndSet(0L)));
        metrics.addCustomChart(new SingleLineChart("blocked_mcleaks", () -> (int) blockedMCLeaks.getAndSet(0L)));
        metrics.addCustomChart(new AdvancedPie("storage", () -> {
            Map<String, Integer> retVal = new HashMap<>();
            for (StorageService service : ConfigUtil.getCachedConfig().getStorage()) {
                retVal.compute(service.getClass().getSimpleName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new AdvancedPie("messaging", () -> {
            Map<String, Integer> retVal = new HashMap<>();
            for (MessagingService service : ConfigUtil.getCachedConfig().getMessaging()) {
                retVal.compute(service.getClass().getSimpleName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new AdvancedPie("sources", () -> {
            Map<String, Integer> retVal = new HashMap<>();
            List<Source<? extends SourceModel>> sources = VPNAPIProvider.getInstance().getSourceManager().getSources();
            for (Source<? extends SourceModel> source : sources) {
                retVal.compute(source.getName(), (k, v) -> {
                    if (v == null) {
                        return 1;
                    }
                    return v + 1;
                });
            }
            if (retVal.isEmpty()) {
                retVal.put("None", 1);
            }

            return retVal;
        }));
        metrics.addCustomChart(new SimplePie("algorithm", () -> ConfigUtil.getCachedConfig().getVPNAlgorithmMethod().getName()));
        metrics.addCustomChart(new SimplePie("vpn_action", () -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            if (!cachedConfig.getVPNKickMessage().isEmpty() && !cachedConfig.getVPNActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.getVPNKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.getVPNActionCommands().isEmpty()) {
                return "commands";
            }
            return "none";
        }));
        metrics.addCustomChart(new SimplePie("mcleaks_action", () -> {
            CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

            if (!cachedConfig.getMCLeaksKickMessage().isEmpty() && !cachedConfig.getMCLeaksActionCommands().isEmpty()) {
                return "multi";
            } else if (!cachedConfig.getMCLeaksKickMessage().isEmpty()) {
                return "kick";
            } else if (!cachedConfig.getMCLeaksActionCommands().isEmpty()) {
                return "commands";
            }
            return "none";
        }));

        PluginHooks.getHooks().add(this);
    }

    @Override
    public void cancel() { }
}
