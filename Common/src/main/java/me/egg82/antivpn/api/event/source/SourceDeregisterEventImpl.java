package me.egg82.antivpn.api.event.source;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.type.AbstractCancellable;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.jetbrains.annotations.NotNull;

public class GenericSourceDeregisterEvent extends AbstractCancellable implements SourceDeregisterEvent {
    private final Source<? extends SourceModel> source;

    public GenericSourceDeregisterEvent(@NotNull VPNAPI api, @NotNull Source<? extends SourceModel> source) {
        super(api);
        this.source = source;
    }

    public @NotNull Source<? extends SourceModel> getSource() { return source; }
}
