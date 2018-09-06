package me.egg82.avpn.commands;

import java.util.Optional;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

import me.egg82.avpn.VPNAPI;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Plugin;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.utils.ThreadUtil;

public class AVPNTestCommand extends AsyncCommandHandler {
	//vars
    private VPNAPI api = VPNAPI.getInstance();
	
	//constructor
	public AVPNTestCommand() {
		super();
	}
	
	//public
	
	//private
	protected void onExecute(long elapsedMilliseconds) {
		if (!sender.hasPermission("avpn.admin")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
			return;
		}
		if (args.length != 1) {
			sender.sendMessage(ChatColor.RED + "Incorrect command usage!");
			String name = getClass().getSimpleName();
			name = name.substring(0, name.length() - 7).toLowerCase();
			ServiceLocator.getService(Plugin.class).getProxy().getPluginManager().dispatchCommand((CommandSender) sender.getHandle(), "? " + name);
			return;
		}
		
		sender.sendMessage(ChatColor.YELLOW + "Test starting..");
		ThreadUtil.submit(new Runnable() {
            public void run() {
                ImmutableMap<String, Optional<Boolean>> map = api.test(args[0]);
                for (Entry<String, Optional<Boolean>> kvp : map.entrySet()) {
                    Boolean bool = kvp.getValue().orElse(null);
                    sender.sendMessage(ChatColor.YELLOW + kvp.getKey() + ": " + ((bool == null) ? ChatColor.YELLOW + "Source error" : ((bool.booleanValue()) ? ChatColor.RED + "VPN/Proxy detected": ChatColor.GREEN + "No VPN/Proxy detected")));
                }
                sender.sendMessage(ChatColor.GREEN + "Test complete!");
            }
		});
	}
	protected void onUndo() {
		
	}
}
