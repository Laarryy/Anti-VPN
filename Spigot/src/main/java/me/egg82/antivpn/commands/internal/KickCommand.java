package me.egg82.antivpn.commands.internal;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.context.CommandContext;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.model.ip.IPManager;
import me.egg82.antivpn.api.model.player.PlayerManager;
import me.egg82.antivpn.bukkit.BukkitCommandUtil;
import me.egg82.antivpn.commands.arguments.KickType;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.locale.BukkitLocalizedCommandSender;
import me.egg82.antivpn.locale.MessageKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.craftbukkit.BukkitComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class KickCommand extends AbstractCommand {
    private final Plugin plugin;

    public KickCommand(@NotNull BukkitCommandManager<BukkitLocalizedCommandSender> commandManager, @NotNull Plugin plugin) {
        super(commandManager);
        this.plugin = plugin;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void execute(@NonNull CommandContext<BukkitLocalizedCommandSender> commandContext) {
        commandManager.taskRecipe().begin(commandContext)
                .synchronous(c -> {
                    CachedConfig cachedConfig = ConfigUtil.getCachedConfig();

                    SinglePlayerSelector selector = c.get("player");
                    Player player = selector.getPlayer();
                    if (player == null) {
                        c.getSender().sendMessage(MessageKey.COMMAND__KICK__ERROR__NOT_ONLINE, "{player}", selector.getSelector());
                        return;
                    }

                    String ip = getIp(player);
                    if (ip == null) {
                        logger.error(c.getSender().getLocalizedText(MessageKey.ERROR__NO_IP, "{player}", player.getName()));
                        c.getSender().sendMessage(MessageKey.ERROR__INTERNAL);
                        return;
                    }

                    KickType type = c.get("type");
                    if (type == KickType.VPN) {
                        IPManager ipManager = VPNAPIProvider.getInstance().getIPManager();

                        if (cachedConfig.getVPNActionCommands().isEmpty() && cachedConfig.getVPNKickMessage().isEmpty()) {
                            c.getSender().sendMessage(MessageKey.COMMAND__KICK__ERROR__VPN_API_MODE);
                            return;
                        }
                        BukkitCommandUtil.dispatchCommands(
                                ipManager.getVpnCommands(player.getName(), player.getUniqueId(), ip),
                                Bukkit.getConsoleSender(),
                                plugin,
                                false
                        );
                        Component kickMessage = ipManager.getVpnKickMessage(player.getName(), player.getUniqueId(), ip);
                        if (kickMessage != null) {
                            player.kickPlayer(BukkitComponentSerializer.legacy().serialize(kickMessage));
                        }

                        c.getSender().sendMessage(MessageKey.COMMAND__KICK__VPN_USAGE, "{player}", player.getName());
                    } else if (type == KickType.MCLEAKS) {
                        PlayerManager playerManager = VPNAPIProvider.getInstance().getPlayerManager();

                        if (cachedConfig.getMCLeaksActionCommands().isEmpty() && cachedConfig.getMCLeaksKickMessage().isEmpty()) {
                            c.getSender().sendMessage(MessageKey.COMMAND__KICK__ERROR__MCLEAKS_API_MODE);
                            return;
                        }
                        BukkitCommandUtil.dispatchCommands(
                                playerManager.getMcLeaksCommands(player.getName(), player.getUniqueId(), ip),
                                Bukkit.getConsoleSender(),
                                plugin,
                                false
                        );
                        Component kickMessage = playerManager.getMcLeaksKickMessage(player.getName(), player.getUniqueId(), ip);
                        if (kickMessage != null) {
                            player.kickPlayer(BukkitComponentSerializer.legacy().serialize(kickMessage));
                        }

                        c.getSender().sendMessage(MessageKey.COMMAND__KICK__MCLEAKS_USAGE, "{player}", player.getName());
                    }
                })
                .execute();
    }

    private @Nullable String getIp(@NotNull Player player) {
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
