package me.egg82.antivpn.services.lookup;

import com.google.common.collect.ImmutableList;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface PlayerInfo {
    @NonNull String getName();
    @NonNull UUID getUUID();

    @NonNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties();
}
