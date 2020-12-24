package me.egg82.antivpn.services.lookup;

import com.google.common.collect.ImmutableList;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.models.ProfileModel;

public interface PlayerInfo {
    String getName();
    UUID getUUID();

    ImmutableList<ProfileModel.ProfilePropertyModel> getProperties();
}
