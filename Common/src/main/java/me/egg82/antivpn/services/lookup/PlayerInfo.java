package me.egg82.antivpn.services.lookup;

import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public interface PlayerInfo {
    @NotNull String getName();

    @NotNull UUID getUUID();

    @NotNull List<ProfileModel.@NotNull ProfilePropertyModel> getProperties();
}
