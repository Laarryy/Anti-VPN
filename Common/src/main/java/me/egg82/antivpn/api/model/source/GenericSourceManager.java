package me.egg82.antivpn.api.model.source;

import java.util.List;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GenericSourceManager implements SourceManager {
    public @Nullable Source<? extends SourceModel> getSource(@NonNull String name) {

    }

    public @Nullable <T extends SourceModel> Source<T> getSource(@NonNull String name, @NonNull Class<T> modelClass) {

    }

    public boolean registerSource(@NonNull Source<? extends SourceModel> source, int index) {

    }

    public boolean deregisterSource(@NonNull String sourceName) {

    }

    public @NonNull List<Source<? extends SourceModel>> getSources() {

    }
}
