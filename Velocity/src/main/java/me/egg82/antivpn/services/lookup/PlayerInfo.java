package me.egg82.antivpn.services.lookup;

import com.google.common.collect.ImmutableList;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.UUID;

public interface PlayerInfo {
    @NonNull String getName();
    @NonNull UUID getUUID();

    @NonNull ImmutableList<ProfileModel.ProfilePropertyModel> getProperties();
}
