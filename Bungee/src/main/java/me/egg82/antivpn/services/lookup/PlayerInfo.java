package me.egg82.antivpn.services.lookup;

import com.google.common.collect.ImmutableList;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.jetbrains.annotations.NotNull;

public interface PlayerInfo {
    @NotNull String getName();
    @NotNull UUID getUUID();

    @NotNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties();
}
