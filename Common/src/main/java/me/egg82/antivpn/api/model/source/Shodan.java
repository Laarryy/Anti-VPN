package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.ShodanModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class Shodan extends AbstractSource<ShodanModel> {
    public @NotNull String getName() { return "shodan"; }

    public boolean isKeyRequired() { return true; }

    public Shodan() {
        super(ShodanModel.class);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
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

    public @NotNull CompletableFuture<@NotNull ShodanModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("https://api.shodan.io/shodan/host/" + ip + "?minify=true&key=" + key);
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<ShodanModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), ShodanModel.class);
        });
    }
}
