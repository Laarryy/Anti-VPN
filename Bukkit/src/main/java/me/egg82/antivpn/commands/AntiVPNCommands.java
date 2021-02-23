package me.egg82.antivpn.commands;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import me.egg82.antivpn.commands.internal.ReloadCommand;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.lang.BukkitLocaleCommandUtil;
import me.egg82.antivpn.lang.BukkitLocalizedCommandSender;
import me.egg82.antivpn.lang.MessageKey;
import me.egg82.antivpn.logging.GELFLogger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public class AntiVPNCommands extends CommandHolder {
    public AntiVPNCommands(@NotNull Plugin plugin) {
        super();

        PaperCommandManager<BukkitLocalizedCommandSender> commandManager = BukkitLocaleCommandUtil.getCommandManager();

        ConfigurationNode config = ConfigUtil.getConfig();

        String[] baseAliases = getAliases(config, "base");

        commands.add(
            commandManager.commandBuilder("antivpn", baseAliases)
                .literal("reload", ArgumentDescription.of(MessageKey.COMMAND_DESC__RELOAD), getAliases(config, "reload")) // TODO: Localization
                .permission(ConfigUtil.getCachedConfig().getAdminPermissionNode())
                .handler(new ReloadCommand(commandManager, plugin.getDataFolder(), plugin))
                .build()
        );

        MinecraftHelp<BukkitLocalizedCommandSender> help = new MinecraftHelp<>(
            "/antivpn help",
            BukkitLocalizedCommandSender::getMappedAudience,
            commandManager
        );

        commandManager.command(
            commandManager.commandBuilder("antivpn", baseAliases)
                .literal("help", ArgumentDescription.of(MessageKey.COMMAND_DESC__RELOAD), getAliases(config, "help")) // TODO: Localization
                .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
                .handler(context -> help.queryCommands(context.getOrDefault("query", ""), context.getSender()))
        );

        registerAll();
    }

    private @NotNull String @NotNull [] getAliases(@NotNull ConfigurationNode config, @NotNull String command) {
        Set<String> retVal;
        try {
            retVal = new HashSet<>(!config.node("aliases", command).empty() ? config.node("aliases", command).getList(String.class) : new ArrayList<>());
        } catch (SerializationException ex) {
            GELFLogger.exception(logger, ex);
            retVal = new HashSet<>();
        }
        retVal.removeIf(action -> action == null || action.isEmpty());

        return retVal.toArray(new String[0]);
    }
}
