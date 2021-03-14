package me.egg82.antivpn.api.event.source;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.type.AbstractCancellable;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SourceDeregisterEventImpl extends AbstractCancellable implements SourceDeregisterEvent {
    private final Source<? extends SourceModel> source;

    public SourceDeregisterEventImpl(@NotNull VPNAPI api, @NotNull Source<? extends SourceModel> source) {
        super(api);
        this.source = source;
    }

    @Override
    public @NotNull Source<? extends SourceModel> getSource() { return source; }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SourceDeregisterEventImpl)) {
            return false;
        }
        SourceDeregisterEventImpl that = (SourceDeregisterEventImpl) o;
        return source.equals(that.source);
    }

    public int hashCode() { return Objects.hash(source); }

    public String toString() {
        return "SourceDeregisterEventImpl{" +
                "api=" + api +
                ", source=" + source +
                ", cancelState=" + cancelState +
                '}';
    }
}
