package me.egg82.antivpn.api.event.source;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.type.AbstractCancellable;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GenericSourceRegisterEvent extends AbstractCancellable implements SourceRegisterEvent {
    private final Source<? extends SourceModel> source;

    public GenericSourceRegisterEvent(@NonNull VPNAPI api, @NonNull Source<? extends SourceModel> source) {
        super(api);
        this.source = source;
    }

    public @NonNull Source<? extends SourceModel> getSource() { return source; }
}
