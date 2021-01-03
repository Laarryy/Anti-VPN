package me.egg82.antivpn.api.model.source;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.event.source.GenericSourceDeregisterEvent;
import me.egg82.antivpn.api.event.source.GenericSourceRegisterEvent;
import me.egg82.antivpn.api.event.type.Cancellable;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import net.engio.mbassy.bus.IMessagePublication;
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

            try {
                VPNAPI api = VPNAPIProvider.getInstance();
                IMessagePublication publication = api.getEventBus().post(new GenericSourceRegisterEvent(api, source)).now();
                if (!publication.isDeadMessage() && ((Cancellable) publication.getMessage()).isCancelled()) {
                    return false;
                }
            } catch (IllegalStateException ignored) { }

            sources.add(index, source);
            return true;
        }
    }

    public boolean deregisterSource(@NonNull Source<? extends SourceModel> source) {
        synchronized (sourceLock) {
            for (Iterator<Source<? extends SourceModel>> i = sources.iterator(); i.hasNext();) {
                Source<? extends SourceModel> s = i.next();
                if (s.getName().equals(source.getName())) {
                    try {
                        VPNAPI api = VPNAPIProvider.getInstance();
                        IMessagePublication publication = api.getEventBus().post(new GenericSourceDeregisterEvent(api, source)).now();
                        if (!publication.isDeadMessage() && ((Cancellable) publication.getMessage()).isCancelled()) {
                            return false;
                        }
                    } catch (IllegalStateException ignored) { }

                    i.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public @NonNull List<Source<? extends SourceModel>> getSources() { return ImmutableList.copyOf(sources); }
}
