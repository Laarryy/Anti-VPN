package me.egg82.antivpn.api.model.source;

import com.google.common.collect.ImmutableList;
import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.VPNAPIProvider;
import me.egg82.antivpn.api.event.source.SourceDeregisterEvent;
import me.egg82.antivpn.api.event.source.SourceDeregisterEventImpl;
import me.egg82.antivpn.api.event.source.SourceRegisterEvent;
import me.egg82.antivpn.api.event.source.SourceRegisterEventImpl;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.utils.EventUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SourceManagerImpl implements SourceManager {
    private final List<Source<? extends SourceModel>> sources = new CopyOnWriteArrayList<>();
    private final Object sourceLock = new Object();

    public @Nullable Source<? extends SourceModel> getSource(@NotNull String name) {
        for (Source<? extends SourceModel> source : sources) {
            if (source.getName().equals(name)) {
                return source;
            }
        }
        return null;
    }

    public @Nullable <T extends SourceModel> Source<T> getSource(@NotNull String name, @NotNull Class<T> modelClass) {
        for (Source<? extends SourceModel> source : sources) {
            if (source.getName().equals(name) && source.getModelClass().isAssignableFrom(modelClass)) {
                return (Source<T>) source;
            }
        }
        return null;
    }

    public boolean registerSource(@NotNull Source<? extends SourceModel> source, int index) {
        synchronized (sourceLock) {
            for (Source<? extends SourceModel> s : sources) {
                if (s.getName().equals(source.getName())) {
                    return false;
                }
            }

            try {
                VPNAPI api = VPNAPIProvider.getInstance();
                SourceRegisterEvent event = new SourceRegisterEventImpl(api, source);
                EventUtil.post(event, api.getEventBus());
                if (event.isCancelled()) {
                    return false;
                }
            } catch (IllegalStateException ignored) {
            }

            sources.add(index, source);
            return true;
        }
    }

    public boolean deregisterSource(@NotNull Source<? extends SourceModel> source) {
        synchronized (sourceLock) {
            for (Iterator<Source<? extends SourceModel>> i = sources.iterator(); i.hasNext(); ) {
                Source<? extends SourceModel> s = i.next();
                if (s.getName().equals(source.getName())) {
                    try {
                        VPNAPI api = VPNAPIProvider.getInstance();

                        SourceDeregisterEvent event = new SourceDeregisterEventImpl(api, source);
                        EventUtil.post(event, api.getEventBus());
                        if (event.isCancelled()) {
                            return false;
                        }
                    } catch (IllegalStateException ignored) {
                    }

                    i.remove();
                    return true;
                }
            }
            return false;
        }
    }

    public @NotNull List<@NotNull Source<? extends SourceModel>> getSources() {
        return ImmutableList.copyOf(sources);
    }
}
