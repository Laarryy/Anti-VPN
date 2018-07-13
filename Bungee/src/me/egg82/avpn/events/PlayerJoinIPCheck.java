package me.egg82.avpn.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.enums.PermissionsType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.async.LowAsyncEventHandler;
import ninja.egg82.utils.ThreadUtil;

public class PlayerJoinIPCheck extends LowAsyncEventHandler<PostLoginEvent> {
	//vars
	private VPNAPI api = VPNAPI.getInstance();
	
	//constructor
	public PlayerJoinIPCheck() {
		super();
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		if (!Config.kick) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo("Plugin set to API-only. Ignoring " + event.getPlayer().getName());
			}
			return;
		}
		
		if (event.getPlayer().hasPermission(PermissionsType.BYPASS)) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo(event.getPlayer().getName() + " bypasses check. Ignoring.");
			}
			return;
		}
		
		String ip = getIp(event.getPlayer());
		
		if (ip == null || ip.isEmpty()) {
			return;
		}
		
		if (Config.async) {
			ThreadUtil.submit(new Runnable() {
				public void run() {
					// We're passing the player object directly because it's unlikely this operation will take long, even in worst-cases
					// Potential memory leaks from keeping the object referenced shouldn't apply here
					checkVPN(event.getPlayer(), ip);
				}
			});
		} else {
			checkVPN(event.getPlayer(), ip);
		}
	}
	
	private String getIp(ProxiedPlayer player) {
		if (player == null) {
			return null;
		}
		
		InetSocketAddress socket = player.getAddress();
		
		if (socket == null) {
			return null;
		}
		
		InetAddress address = socket.getAddress();
		if (address == null) {
			return null;
		}
		
		return address.getHostAddress();
	}
	private void checkVPN(ProxiedPlayer player, String ip) {
		if (api.isVPN(ip, true)) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getName() + " found using a VPN. Kicking with defined message.");
			}
			player.disconnect(new TextComponent(Config.kickMessage));
		} else {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getName() + " passed VPN check.");
			}
		}
	}
}
