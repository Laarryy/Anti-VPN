package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPHunterModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class IPHunter extends AbstractSource<IPHunterModel> {
    public @NotNull String getName() { return "iphunter"; }

    public boolean isKeyRequired() { return true; }

    public IPHunter() {
        super(IPHunterModel.class);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"success".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(model.getCode() != null && model.getCode().contains("X-Key"), "Could not get result from " + getName() + " (" + model.getCode() + ")");
            }

            return model.getData() != null && (model.getData().getBlock() == getSourceConfigNode().node("block").getInt(1));
        });
    }

    public @NotNull CompletableFuture<@NotNull IPHunterModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("https://www.iphunter.info:8082/v1/ip/" + ip);
            builder.header("X-Key", key);
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPHunterModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPHunterModel.class);
        });
    }
}
