package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.ShodanModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class Shodan extends AbstractSource<ShodanModel> {
    public @NonNull String getName() { return "shodan"; }

    public boolean isKeyRequired() { return true; }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getError() != null) {
                throw new APIException(model.getError().contains("key"), "Could not get result from " + getName() + " (" + model.getError() + ")");
            }

            if (model.getTags() == null || model.getTags().isEmpty()) {
                return false;
            }

            for (String tag : model.getTags()) {
                if ("proxy".equalsIgnoreCase(tag) || "vpn".equalsIgnoreCase(tag)) {
                    return true;
                }
            }
            return false;
        });
    }

    public @NonNull CompletableFuture<ShodanModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            HttpURLConnection conn = getConnection("https://api.shodan.io/shodan/host/" + ip + "?minify=true&key=" + key, "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", headers);
            JSONDeserializer<ShodanModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), ShodanModel.class);
        });
    }
}
