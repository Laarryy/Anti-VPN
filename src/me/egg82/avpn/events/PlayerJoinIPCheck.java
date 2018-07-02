package me.egg82.avpn.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.enums.PermissionsType;
import ninja.egg82.bukkit.BasePlugin;
import ninja.egg82.bukkit.utils.TaskUtil;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.events.LowEventHandler;
import ninja.egg82.utils.ThreadUtil;

public class PlayerJoinIPCheck extends LowEventHandler<PlayerJoinEvent> {
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
				ServiceLocator.getService(BasePlugin.class).printInfo("Plugin set to API-only. Ignoring " + event.getPlayer().getName());
			}
			return;
		}
		
		if (event.getPlayer().hasPermission(PermissionsType.BYPASS)) {
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(event.getPlayer().getName() + " bypasses check. Ignoring.");
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
					checkVPN(event.getPlayer(), ip, true);
				}
			});
		} else {
			checkVPN(event.getPlayer(), ip, false);
		}
	}
	
	private String getIp(Player player) {
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
	private void checkVPN(Player player, String ip, boolean isAsync) {
		if (api.isVPN(ip, true)) {
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(player.getName() + " found using a VPN. Kicking with defined message.");
			}
			if (isAsync) {
				// We're not allowed to kick players off the main thread
				TaskUtil.runSync(new Runnable() {
					public void run() {
						player.kickPlayer(Config.kickMessage);
					}
				});
			} else {
				player.kickPlayer(Config.kickMessage);
			}
		} else {
			if (Config.debug) {
				ServiceLocator.getService(BasePlugin.class).printInfo(player.getName() + " passed VPN check.");
			}
		}
	}
}
