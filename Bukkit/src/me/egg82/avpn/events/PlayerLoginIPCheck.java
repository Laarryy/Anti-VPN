package me.egg82.avpn.events;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.debug.IDebugPrinter;
import me.egg82.avpn.enums.PermissionsType;
import me.egg82.avpn.registries.UUIDIPRegistry;
import ninja.egg82.bukkit.utils.TaskUtil;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.patterns.registries.IRegistry;
import ninja.egg82.plugin.handlers.events.LowEventHandler;
import ninja.egg82.utils.ThreadUtil;

public class PlayerLoginIPCheck extends LowEventHandler<PlayerLoginEvent> {
	//vars
	private VPNAPI api = VPNAPI.getInstance();
	
	private IRegistry<UUID, String> uuidIpRegistry = ServiceLocator.getService(UUIDIPRegistry.class);
	
	//constructor
	public PlayerLoginIPCheck() {
		super();
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		String ip = uuidIpRegistry.removeRegister(event.getPlayer().getUniqueId());
		
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
	
	private void checkVPN(Player player, String ip, boolean isAsync) {
		if (api.isVPN(ip, true)) {
			if (Config.debug) {
				ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getName() + " found using a VPN. Kicking with defined message.");
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
				ServiceLocator.getService(IDebugPrinter.class).printInfo(player.getName() + " passed VPN check.");
			}
		}
	}
}
