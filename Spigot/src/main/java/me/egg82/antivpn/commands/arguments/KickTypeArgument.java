package me.egg82.antivpn.commands.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.captions.StandardCaptionKeys;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import com.google.common.collect.ImmutableList;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

// https://github.com/Incendo/cloud/blob/master/cloud-core/src/main/java/cloud/commandframework/arguments/standard/UUIDArgument.java
public class KickTypeArgument<C> extends CommandArgument<C, KickType> {
    private KickTypeArgument(
            boolean required,
            @NonNull String name,
            @NotNull String defaultValue,
            @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
            ArgumentDescription defaultDescription
    ) {
        super(required, name, new KickTypeParser<>(), defaultValue, KickType.class, suggestionsProvider, defaultDescription);
    }

    public static <C> @NotNull Builder<C> newBuilder(@NotNull String name) { return new Builder<>(name); }

    public static <C> @NotNull CommandArgument<C, KickType> of(@NotNull String name) { return KickTypeArgument.<C>newBuilder(name).asRequired().build(); }

    public static <C> @NotNull CommandArgument<C, KickType> optional(@NotNull String name) { return KickTypeArgument.<C>newBuilder(name).asOptional().build(); }

    public static <C> @NotNull CommandArgument<C, KickType> optional(
            String name,
            KickType defaultKickType
    ) { return KickTypeArgument.<C>newBuilder(name).asOptionalWithDefault(defaultKickType.name()).build(); }

    public static class Builder<C> extends CommandArgument.Builder<C, KickType> {
        private Builder(@NotNull String name) {
            super(KickType.class, name);
        }

        @Override
        public @NotNull KickTypeArgument<C> build() {
            return new KickTypeArgument<>(
                    this.isRequired(),
                    this.getName(),
                    this.getDefaultValue(),
                    this.getSuggestionsProvider(),
                    this.getDefaultDescription()
            );
        }
    }

    public static class KickTypeParser<C> implements ArgumentParser<C, KickType> {
        @Override
        public @NotNull ArgumentParseResult<KickType> parse(@NotNull CommandContext<C> commandContext, @NotNull Queue<String> inputQueue) {
            System.out.println("kick parser inputQueue: \"" + Arrays.toString(inputQueue.toArray(new String[0])) + "\"");
            String arg = inputQueue.poll();
            if (arg == null) {
                return ArgumentParseResult.failure(new NoInputProvidedException(KickTypeParser.class, commandContext));
            }

            if ("vpn".equalsIgnoreCase(arg)) {
                return ArgumentParseResult.success(KickType.VPN);
            } else if ("mcleaks".equalsIgnoreCase(arg)) {
                return ArgumentParseResult.success(KickType.MCLEAKS);
            }
            return ArgumentParseResult.failure(new KickParseException(arg, commandContext));
        }

        @Override
        public @NotNull List<String> suggestions(@NotNull CommandContext<C> commandContext, @NotNull String input) {
            if (input.isEmpty()) {
                return ImmutableList.of("vpn", "mcleaks");
            }
            input = input.toLowerCase();
            if ("vpn".startsWith(input)) {
                return ImmutableList.of("vpn");
            } else if ("mcleaks".startsWith(input)) {
                return ImmutableList.of("mcleaks");
            }
            return ImmutableList.of();
        }

        @Override
        public boolean isContextFree() { return true; }
    }

    public static class KickParseException extends ParserException {
        private final String input;

        public KickParseException(@NotNull String input, @NotNull CommandContext<?> commandContext) {
            super(KickTypeParser.class, commandContext, StandardCaptionKeys.ARGUMENT_PARSE_FAILURE_ENUM, CaptionVariable.of("input", input));
            this.input = input;
        }

        public @NotNull String getInput() { return input; }
    }
}
