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
    private final @NotNull List<@NotNull Source<SourceModel>> sources = new CopyOnWriteArrayList<>();
    private final @NotNull Object sourceLock = new Object();

    @Override
    @Nullable
    public Source<SourceModel> getSource(@NotNull String name) {
        for (Source<SourceModel> source : sources) {
            if (source.getName().equals(name)) {
                return source;
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends SourceModel> Source<T> getSource(@NotNull String name, @NotNull Class<T> modelClass) {
        for (Source<SourceModel> source : sources) {
            if (source.getName().equals(name) && source.getModelClass().isAssignableFrom(modelClass)) {
                return (Source<T>) source;
            }
        }
        return null;
    }

    @Override
    public boolean registerSource(@NotNull Source<SourceModel> source, int index) {
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

    @Override
    public boolean deregisterSource(@NotNull Source<SourceModel> source) {
        synchronized (sourceLock) {
            for (Iterator<Source<SourceModel>> i = sources.iterator(); i.hasNext(); ) {
                Source<SourceModel> s = i.next();
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

    @Override
    @NotNull
    public List<@NotNull Source<SourceModel>> getSources() { return ImmutableList.copyOf(sources); }
}
