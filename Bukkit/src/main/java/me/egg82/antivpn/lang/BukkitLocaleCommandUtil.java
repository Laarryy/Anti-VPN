package me.egg82.antivpn.lang;

import cloud.commandframework.bukkit.BukkitCommandManager;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import me.egg82.antivpn.bukkit.BukkitCapabilities;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.config.ConfigurationFileUtil;
import me.egg82.antivpn.logging.GELFLogger;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BukkitLocaleCommandUtil {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(BukkitLocaleCommandUtil.class));

    private static BukkitAudiences adventure;

    private static final MinecraftExceptionHandler<BukkitLocalizedCommandSender> commandExceptionHandler = new MinecraftExceptionHandler<>();
    private static PaperCommandManager<BukkitLocalizedCommandSender> commandManager = null;

    private static BukkitLocalizedCommandSender consoleCommandSender = null;

    private BukkitLocaleCommandUtil() { }

    public static void create(@NotNull Plugin plugin) {
        adventure = BukkitAudiences.create(plugin);

        consoleCommandSender = BukkitLocalizedCommandSender.getMappedCommandSender(
            plugin.getServer().getConsoleSender(),
            adventure.sender(plugin.getServer().getConsoleSender()),
            LocaleUtil.getDefaultI18N()
        );

        try {
            commandManager = new PaperCommandManager<>(
                plugin,
                AsynchronousCommandExecutionCoordinator.<BukkitLocalizedCommandSender>newBuilder().build(),
                s -> BukkitLocalizedCommandSender.getMappedCommandSender(s, adventure.sender(s), getLanguage(plugin, s)),
                BukkitLocalizedCommandSender::getBaseCommandSender
            );
        } catch (Exception ex) {
            logger.error(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__COMMAND_MANAGER), ex);
            return;
        }

        commandExceptionHandler
            .withInvalidSyntaxHandler()
            .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX, (sender, ex) -> {
                logger.error(ex.getMessage(), ex);
                return sender.getComponent(MessageKey.ERROR__COMMAND__INVALID_SYNTAX, "{ex}", ex.getClass().getName() + ": " + ex.getLocalizedMessage());
            })
            .withInvalidSenderHandler()
            .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SENDER, (sender, ex) -> sender.getComponent(MessageKey.ERROR__COMMAND__INVALID_SENDER, "{ex}", ex.getClass().getName() + ": " + ex.getLocalizedMessage()))
            .withNoPermissionHandler()
            .withHandler(MinecraftExceptionHandler.ExceptionType.NO_PERMISSION, (sender, ex) -> sender.getComponent(MessageKey.ERROR__COMMAND__NO_PERMISSION, "{ex}", ex.getClass().getName() + ": " + ex.getLocalizedMessage()))
            .withArgumentParsingHandler()
            .withHandler(MinecraftExceptionHandler.ExceptionType.ARGUMENT_PARSING, (sender, ex) -> {
                logger.error(ex.getMessage(), ex);
                return sender.getComponent(MessageKey.ERROR__COMMAND__INVALID_ARGS, "{ex}", ex.getClass().getName() + ": " + ex.getLocalizedMessage());
            })
            .withCommandExecutionHandler()
            .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION, (sender, ex) -> {
                logger.error(ex.getMessage(), ex);
                return sender.getComponent(MessageKey.ERROR__INTERNAL);
            })
            .withDecorator(component -> Component.text()
                .append(component)
                .build()
            )
            .apply(commandManager, BukkitLocalizedCommandSender::getMappedAudience);

        if (commandManager.queryCapability(CloudBukkitCapabilities.BRIGADIER)) {
            try {
                commandManager.registerBrigadier();
                consoleCommandSender.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "Brigadier");
            } catch (BukkitCommandManager.BrigadierFailureException ex) {
                logger.error(ex.getMessage(), ex);
                consoleCommandSender.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "Brigadier");
            }
        } else {
            consoleCommandSender.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "Brigadier");
        }
        if (commandManager.queryCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            commandManager.registerAsynchronousCompletions();
            consoleCommandSender.sendMessage(MessageKey.GENERAL__ENABLE_HOOK, "{hook}", "Async Paper");
        } else {
            consoleCommandSender.sendMessage(MessageKey.GENERAL__NO_HOOK, "{hook}", "Async Paper");
        }
    }

    public static void close() {
        if (adventure != null) {
            adventure.close();
        }
    }

    public static @NotNull PaperCommandManager<BukkitLocalizedCommandSender> getCommandManager() {
        if (commandManager == null) {
            throw new IllegalStateException("Command manager is null.");
        }
        return commandManager;
    }

    public static @NotNull BukkitLocalizedCommandSender getConsole() {
        if (consoleCommandSender == null) {
            throw new IllegalStateException("Console command sender is null.");
        }
        return consoleCommandSender;
    }

    public static @NotNull BukkitLocalizedCommandSender getSender(@NotNull CommandSender sender) {
        if (commandManager == null) {
            throw new IllegalStateException("Command manager is null.");
        }
        return commandManager.getCommandSenderMapper().apply(sender);
    }

    private static final UUID consoleUuid = new UUID(0L, 0L);

    public static void setConsoleLocale(@NotNull Plugin plugin, @NotNull I18NManager manager) {
        consoleCommandSender = BukkitLocalizedCommandSender.getMappedCommandSender(
            plugin.getServer().getConsoleSender(),
            adventure.sender(plugin.getServer().getConsoleSender()),
            manager
        );
    }

    private static @NotNull I18NManager getLanguage(@NotNull Plugin plugin, @NotNull CommandSender sender) {
        if (sender instanceof Player) {
            return I18NManager.getUserCache().computeIfAbsent(((Player) sender).getUniqueId(), k -> {
                return I18NManager.getManager(
                    plugin.getDataFolder(),
                    BukkitCapabilities.HAS_ADVENTURE ? ((Player) sender).locale() : LocaleUtil.parseLocale(((Player) sender).getLocale())
                );
            });
        } else {
            return I18NManager.getUserCache().computeIfAbsent(consoleUuid, k -> {
                CachedConfig cachedConfig;
                try {
                    cachedConfig = ConfigUtil.getCachedConfig();
                } catch (IllegalStateException ignored) {
                    cachedConfig = null;
                }
                return I18NManager.getManager(
                    plugin.getDataFolder(),
                    cachedConfig != null ? cachedConfig.getLanguage() : Locale.US
                );
            });
        }
    }
}
