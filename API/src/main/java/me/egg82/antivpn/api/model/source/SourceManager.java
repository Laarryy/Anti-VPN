package me.egg82.antivpn.api.model.source;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents the object responsible for managing {@link Source} instances.
 *
 * <p>All blocking methods return {@link CompletableFuture}s, which will be
 * populated with the result once the data has been loaded/saved asynchronously.
 * Care should be taken when using such methods to ensure that the main server
 * thread is not blocked.</p>
 *
 * <p>Methods such as {@link CompletableFuture#get()} and equivalent should
 * <strong>not</strong> be called on the main server thread. If you need to use
 * the result of these operations on the main server thread, register a
 * callback using {@link CompletableFuture#thenAcceptAsync(Consumer, Executor)}.</p>
 */
public interface SourceManager {
    /**
     * Gets a source.
     *
     * @param name the name of the source to get
     * @return a {@link Source} object, if one matching the name is available, or null if not
     * @throws NullPointerException if the name is null
     */
    @Nullable Source<? extends SourceModel> getSource(@NonNull String name);

    /**
     * Gets a source with a model type.
     *
     * @param name the name of the source to get
     * @param modelClass the class of the model to return
     * @return a {@link Source} object, if one matching the name is available, or null if not
     * @throws NullPointerException if the name or model is null
     */
    @Nullable <T extends SourceModel> Source<T> getSource(@NonNull String name, @NonNull Class<T> modelClass);

    /**
     * Gets a list of all enabled {@link Source}s, in the order
     * they are provided in the AntiVPN plugin configuration.
     *
     * @return a list of sources
     */
    @NonNull CompletableFuture<List<Source<? extends SourceModel>>> getSources();
}
