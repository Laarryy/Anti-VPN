package me.egg82.antivpn.services.lookup;

import com.google.common.collect.ImmutableList;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface PlayerInfo {
    @NotNull String getName();

    @NotNull UUID getUUID();

    @NotNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties();
}
