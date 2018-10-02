package me.egg82.avpn.commands;

import com.velocitypowered.api.command.CommandSource;

import me.egg82.avpn.Config;
import me.egg82.avpn.VPNAPI;
import me.egg82.avpn.utils.ValidationUtil;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.handlers.async.AsyncCommandHandler;
import ninja.egg82.utils.ThreadUtil;
import ninja.egg82.velocity.BasePlugin;

public class AVPNCheckCommand extends AsyncCommandHandler {
    // vars
    private VPNAPI api = VPNAPI.getInstance();

    // constructor
    public AVPNCheckCommand() {
        super();
    }

    // public

    // private
    protected void onExecute(long elapsedMilliseconds) {
        if (!sender.hasPermission("avpn.admin")) {
            sender.sendMessage(TextComponent.of("You do not have permission to use this command!", TextColor.RED).content());
            return;
        }
        if (args.length != 1) {
            sender.sendMessage(TextComponent.of("Incorrect command usage!", TextColor.RED).content());
            String name = getClass().getSimpleName();
            name = name.substring(0, name.length() - 7).toLowerCase();
            ServiceLocator.getService(BasePlugin.class).getProxy().getCommandManager().execute((CommandSource) sender.getHandle(), "? " + name);
            return;
        }
        if (!ValidationUtil.isValidIp(args[0])) {
            sender.sendMessage(TextComponent.of("The IP specified isn't valid!", TextColor.RED).content());
            return;
        }

        sender.sendMessage(TextComponent.of("Checking IP..", TextColor.YELLOW).content());
        ThreadUtil.submit(new Runnable() {
            public void run() {
                if (Config.consensus >= 0.0d) {
                    // Consensus algorithm
                    sender.sendMessage((api.consensus(args[0]) >= Config.consensus) ? TextComponent.of("VPN/Proxy detected", TextColor.RED).content()
                        : TextComponent.of("No VPN/Proxy detected", TextColor.GREEN).content());
                } else {
                    // Cascade algorithm
                    sender.sendMessage(api.isVPN(args[0]) ? TextComponent.of("VPN/Proxy detected", TextColor.RED).content() : TextComponent.of("No VPN/Proxy detected", TextColor.GREEN).content());
                }
            }
        });
    }

    protected void onUndo() {

    }
}
