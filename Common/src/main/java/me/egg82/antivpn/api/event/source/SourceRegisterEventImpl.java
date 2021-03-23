package me.egg82.antivpn.api.event.source;

import me.egg82.antivpn.api.VPNAPI;
import me.egg82.antivpn.api.event.type.AbstractCancellable;
import me.egg82.antivpn.api.model.source.Source;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class SourceRegisterEventImpl extends AbstractCancellable implements SourceRegisterEvent {
    private final @NotNull Source<SourceModel> source;

    public SourceRegisterEventImpl(@NotNull VPNAPI api, @NotNull Source<SourceModel> source) {
        super(api);
        this.source = source;
    }

    @Override
    @NotNull
    public Source<SourceModel> getSource() { return source; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SourceRegisterEventImpl)) {
            return false;
        }
        SourceRegisterEventImpl that = (SourceRegisterEventImpl) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() { return Objects.hash(source); }

    @Override
    public String toString() {
        return "SourceRegisterEventImpl{" +
                "api=" + api +
                ", source=" + source +
                ", cancelState=" + cancelState +
                '}';
    }
}
