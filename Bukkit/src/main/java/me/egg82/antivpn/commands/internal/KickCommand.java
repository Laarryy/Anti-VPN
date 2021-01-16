package me.egg82.antivpn.commands.internal;

import co.aikar.commands.CommandIssuer;
import co.aikar.taskchain.TaskChainFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.Message;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class KickCommand extends AbstractCommand {
    private final String player;
    private final String type;

    public KickCommand(@NonNull CommandIssuer issuer, @NonNull TaskChainFactory taskFactory, @NonNull String player, @NonNull String type) {
        super(issuer, taskFactory);
        this.player = player;
        this.type = type;
    }

    public void run() {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            logger.error("Cached config could not be fetched.");
            issuer.sendError(Message.ERROR__INTERNAL);
            return;
        }

        taskFactory.<Void>newChain()
                .<UUID>asyncCallback((v, r) -> r.accept(fetchUuid(player)))
                .abortIfNull(this.handleAbort)
                .syncLast(v -> {
                    Player p = Bukkit.getPlayer(v);
                    if (p == null) {
                        issuer.sendError(Message.KICK__NO_PLAYER);
                        return;
                    }

                    String ip = getIp(p);
                    if (ip == null) {
                        logger.error("Could not get IP for player " + p.getName());
                        issuer.sendError(Message.ERROR__INTERNAL);
                        return;
                    }

                    if (type.equalsIgnoreCase("vpn")) {
                        IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

                        if (cachedConfig.getVPNActionCommands().isEmpty() && cachedConfig.getVPNKickMessage().isEmpty()) {
                            issuer.sendError(Message.KICK__API_MODE);
                            return;
                        }
                        List<String> commands = ipManager.getVpnCommands(p.getName(), p.getUniqueId(), ip);
                        for (String command : commands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        }
                        String kickMessage = ipManager.getVpnKickMessage(p.getName(), p.getUniqueId(), ip);
                        if (kickMessage != null) {
                            p.kickPlayer(kickMessage);
                        }
                    } else if (type.equalsIgnoreCase("mcleaks")) {
                        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

                        if (cachedConfig.getMCLeaksActionCommands().isEmpty() && cachedConfig.getMCLeaksKickMessage().isEmpty()) {
                            issuer.sendError(Message.KICK__API_MODE);
                            return;
                        }
                        List<String> commands = playerManager.getMcLeaksCommands(p.getName(), p.getUniqueId(), ip);
                        for (String command : commands) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        }
                        String kickMessage = playerManager.getMcLeaksKickMessage(p.getName(), p.getUniqueId(), ip);
                        if (kickMessage != null) {
                            p.kickPlayer(kickMessage);
                        }
                    }

                    issuer.sendInfo(Message.KICK__END_VPN, "{player}", player);
                })
                .execute();
    }

    private @Nullable String getIp(@NonNull Player player) {
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
}
