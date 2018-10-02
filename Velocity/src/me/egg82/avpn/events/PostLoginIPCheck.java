package me.egg82.avpn.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.proxy.Player;

import me.egg82.avpn.AnalyticsData;
import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.enums.PermissionsType;
import net.kyori.text.TextComponent;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.async.LowAsyncEventHandler;
import ninja.egg82.utils.ThreadUtil;

public class PostLoginIPCheck extends LowAsyncEventHandler<PostLoginEvent> {
    // vars
    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public PostLoginIPCheck() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!Config.kick) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo("Plugin set to API-only. Ignoring " + event.getPlayer().getUsername());
            }
            return;
        }

        if (event.getPlayer().hasPermission(PermissionsType.BYPASS)) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(event.getPlayer().getUsername() + " bypasses check. Ignoring.");
            }
            return;
        }

        String ip = getIp(event.getPlayer());

        if (ip == null || ip.isEmpty()) {
            return;
        }

        if (Config.ignore.contains(ip)) {
            if (Config.debug) {
                ServiceLocator.getService(IDebugPrinter.class).printInfo(event.getPlayer().getUsername() + " is using an ignored ip \"" + ip + "\". Ignoring.");
            }
            return;
        }

        if (Config.async) {
            ThreadUtil.submit(new Runnable() {
                public void run() {
                    // We're passing the player object directly because it's unlikely this operation
                    // will take long, even in worst-cases
                    // Potential memory leaks from keeping the object referenced shouldn't apply
                    // here
                    checkVPN(event.getPlayer(), ip);
                }
            });
        } else {
            checkVPN(event.getPlayer(), ip);
        }
    }

    private String getIp(Player player) {
        if (player == null) {
            return null;
        }

        InetSocketAddress socket = player.getRemoteAddress();

        if (socket == null) {
            return null;
        }

        InetAddress address = socket.getAddress();
        if (address == null) {
            return null;
        }

        return address.getHostAddress();
    }

    private void checkVPN(Player player, String ip) {
        if (Config.consensus >= 0.0d) {
            // Consensus algorithm
            if (api.consensus(ip, true) >= Config.consensus) {
                AnalyticsData.playersBlocked++;
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getUsername() + " found using a VPN. Kicking with defined message.");
                }
                player.disconnect(TextComponent.of(Config.kickMessage));
            } else {
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getUsername() + " passed VPN check.");
                }
            }
        } else {
            // Cascade algorithm
            if (api.isVPN(ip, true)) {
                AnalyticsData.playersBlocked++;
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getUsername() + " found using a VPN. Kicking with defined message.");
                }
                player.disconnect(TextComponent.of(Config.kickMessage));
            } else {
                if (Config.debug) {
                    ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getUsername() + " passed VPN check.");
                }
            }
        }
    }
}
