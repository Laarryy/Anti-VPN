package me.egg82.antivpn.services.lookup;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.egg82.antivpn.services.lookup.models.ProfileModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PaperPlayerInfo extends MojangPlayerInfo {
    PaperPlayerInfo(@NotNull UUID uuid) throws IOException { super(uuid); }

    PaperPlayerInfo(@NotNull String name) throws IOException { super(name); }

    protected @NotNull String nameExpensive(@NotNull UUID uuid) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            nameCache.put(player.getName(), uuid);
            return player.getName();
        }

        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(uuid);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null) {
            nameCache.put(profile.getName(), profile.getId());
            return profile.getName();
        }

        // Network lookup
        if (profile.complete(true) && profile.getName() != null && profile.getId() != null) {
            nameCache.put(profile.getName(), profile.getId());
            return profile.getName();
        }

        // Sorry, nada
        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    protected @NotNull UUID uuidExpensive(@NotNull String name) throws IOException {
        // Currently-online lookup
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            uuidCache.put(player.getUniqueId(), name);
            return player.getUniqueId();
        }

        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(name);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null) {
            uuidCache.put(profile.getId(), profile.getName());
            return profile.getId();
        }

        // Network lookup
        if (profile.complete(true) && profile.getName() != null && profile.getId() != null) {
            uuidCache.put(profile.getId(), profile.getName());
            return profile.getId();
        }

        // Sorry, nada
        throw new IOException("Could not load player data from Mojang (rate-limited?)");
    }

    protected @NotNull List<ProfileModel.@NotNull ProfilePropertyModel> propertiesExpensive(@NotNull UUID uuid) throws IOException {
        // Cached profile lookup
        PlayerProfile profile = Bukkit.createProfile(uuid);
        if ((profile.isComplete() || profile.completeFromCache()) && profile.getName() != null && profile.getId() != null && profile.hasTextures()) {
            return toPropertiesModel(profile.getProperties());
        }

        // Network lookup
        if (profile.complete(true) && profile.getName() != null && profile.getId() != null) {
            return toPropertiesModel(profile.getProperties());
        }

        throw new IOException("Could not load skin data from Mojang (rate-limited?)");
    }

    private static @NotNull List<ProfileModel.@NotNull ProfilePropertyModel> toPropertiesModel(@NotNull Set<ProfileProperty> properties) {
        List<ProfileModel.ProfilePropertyModel> retVal = new ArrayList<>();
        for (ProfileProperty property : properties) {
            ProfileModel.ProfilePropertyModel newProperty = new ProfileModel.ProfilePropertyModel();
            newProperty.setName(property.getName());
            newProperty.setValue(property.getValue());
            newProperty.setSignature(property.getSignature());
            retVal.add(newProperty);
        }
        return retVal;
    }
}
