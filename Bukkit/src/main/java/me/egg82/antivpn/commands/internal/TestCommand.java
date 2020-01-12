package me.egg82.antivpn.commands.internal;

import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainAbortAction;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.VPNAPI;
import me.egg82.antivpn.utils.LogUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Fix
public class TestCommand implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final TaskChain<?> chain;
    private final CommandSender sender;
    private final String ip;

    private final VPNAPI api = VPNAPI.getInstance();

    public TestCommand(TaskChain<?> chain, CommandSender sender, String ip) {
        this.chain = chain;
        this.sender = sender;
        this.ip = ip;
    }

    public void run() {
        sender.sendMessage(LogUtil.getHeading() + ChatColor.YELLOW + "Testing with " + ChatColor.WHITE + ip + ChatColor.YELLOW + ", please wait..");

        /*chain
                .<ImmutableMap<String, Optional<Boolean>>>asyncCallback((v, f) -> {
                    try {
                        f.accept(api.testAllSources(ip));
                        return;
                    } catch (APIException ex) {
                        logger.error(ex.getMessage(), ex);
                    }
                    f.accept(null);
                })
                .abortIfNull(new TaskChainAbortAction<Object, Object, Object>() {
                    @Override
                    public void onAbort(TaskChain<?> chain, Object arg1) {
                        sender.sendMessage(LogUtil.getHeading() + LogUtil.getHeading() + ChatColor.YELLOW + "Internal error");
                    }
                })
                .syncLast(v -> {
                    for (Map.Entry<String, Optional<Boolean>> kvp : v.entrySet()) {
                        if (!kvp.getValue().isPresent()) {
                            sender.sendMessage(LogUtil.getHeading() + LogUtil.getSourceHeading(kvp.getKey()) + ChatColor.YELLOW + "Source error");
                            continue;
                        }

                        sender.sendMessage(LogUtil.getHeading() + LogUtil.getSourceHeading(kvp.getKey()) + (kvp.getValue().get() ? ChatColor.DARK_RED + "VPN/Proxy detected" : ChatColor.GREEN + "No VPN/Proxy detected"));
                    }
                    sender.sendMessage(LogUtil.getHeading() + ChatColor.GREEN + "Test for " + ChatColor.YELLOW + ip + ChatColor.GREEN + " complete!");
                })
                .execute();*/
    }
}
