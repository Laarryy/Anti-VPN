package me.egg82.antivpn.api.model.source;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericSourceManager implements SourceManager {
    private final List<Source<? extends SourceModel>> sources = new CopyOnWriteArrayList<>();
    private final Object sourceLock = new Object();

    public @Nullable Source<? extends SourceModel> getSource(@NonNull String name) {
        for (Source<? extends SourceModel> source : sources) {
            if (source.getName().equals(name)) {
                return source;
            }
        }
        return null;
    }

    public @Nullable <T extends SourceModel> Source<T> getSource(@NonNull String name, @NonNull Class<T> modelClass) {
        for (Source<? extends SourceModel> source : sources) {
            if (source.getName().equals(name) && source.getModelClass().isAssignableFrom(modelClass)) {
                return (Source<T>) source;
            }
        }
        return null;
    }

    public boolean registerSource(@NonNull Source<? extends SourceModel> source, int index) {
        synchronized (sourceLock) {
            for (Source<? extends SourceModel> s : sources) {
                if (s.getName().equals(source.getName())) {
                    return false;
                }
            }
            sources.add(index, source);
            return true;
        }
    }

    public boolean deregisterSource(@NonNull String sourceName) {
        synchronized (sourceLock) {
            for (Iterator<Source<? extends SourceModel>> i = sources.iterator(); i.hasNext();) {
                Source<? extends SourceModel> source = i.next();
                if (source.getName().equals(sourceName)) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public @NonNull List<Source<? extends SourceModel>> getSources() { return ImmutableList.copyOf(sources); }
}
