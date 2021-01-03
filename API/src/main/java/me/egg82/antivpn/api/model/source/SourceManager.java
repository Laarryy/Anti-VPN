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
     * Registers and enables a new, unique {@link Source}, replacing
     * an existing source with the same name in the sources list
     * (the order in which they will be used).
     *
     * <p>Note that this method will return false if a source with
     * the same name was not registered.</p>
     *
     * @param newSource The new source to replace an existing source with
     * @return true if the replacement was successful, false if not
     * @throws NullPointerException if the new source is null
     */
    default boolean replaceSource(@NonNull Source<? extends SourceModel> newSource) {
        List<Source<? extends SourceModel>> sources = getSources();
        int index = -1;
        Source<? extends SourceModel> removedSource = null;
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).getName().equals(newSource.getName())) {
                index = i;
                removedSource = sources.get(i);
                break;
            }
        }
        if (index == -1 || removedSource == null || !deregisterSource(removedSource)) {
            return false;
        }
        return registerSource(newSource, index);
    }

    /**
     * Registers and enables a new, unique {@link Source}, appending
     * the source to the end of the sources list (the order in which
     * they will be used).
     *
     * <p>Note that this method will return false if a source with
     * the same name as an existing source is being added.</p>
     *
     * @param source The new, unique source to add
     * @return true if the addition was successful, false if not
     * @throws NullPointerException if the source is null
     */
    default boolean registerSource(@NonNull Source<? extends SourceModel> source) { return registerSource(source, getSources().size()); }

    /**
     * Registers and enables a new, unique {@link Source}, inserting
     * the source at the specified location in the sources list (the
     * order in which they will be used).
     *
     * <p>Note that this method will return false if a source with
     * the same name as an existing source is being added.</p>
     *
     * @param source The new, unique source to add
     * @return true if the addition was successful, false if not
     * @throws NullPointerException if the source is null
     */
    boolean registerSource(@NonNull Source<? extends SourceModel> source, int index);

    /**
     * Deregisters, disables, and removes an existing source from
     * the sources list.
     *
     * <p>Note that this method will return false if the source
     * was not registered.</p>
     *
     * @param source The source to remove
     * @return true if the removal was successful, false if not
     * @throws NullPointerException if the source is null
     */
    boolean deregisterSource(@NonNull Source<? extends SourceModel> source);

    /**
     * Deregisters, disables, and removes an existing source from
     * the sources list.
     *
     * <p>Note that this method will return false if the source
     * was not registered.</p>
     *
     * @param sourceName The source to remove
     * @return true if the removal was successful, false if not
     * @throws NullPointerException if the source name is null
     */
    default boolean deregisterSource(@NonNull String sourceName) {
        List<Source<? extends SourceModel>> sources = getSources();
        Source<? extends SourceModel> removedSource = null;
        for (Source<? extends SourceModel> source : sources) {
            if (source.getName().equals(sourceName)) {
                removedSource = source;
                break;
            }
        }
        if (removedSource != null) {
            return deregisterSource(removedSource);
        }
        return false;
    }

    /**
     * Gets a list of all enabled {@link Source}s, in the order
     * they are used in the Anti-VPN plugin.
     *
     * @return a list of sources
     */
    @NonNull List<Source<? extends SourceModel>> getSources();
}
