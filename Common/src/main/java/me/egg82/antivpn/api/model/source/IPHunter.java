package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPHunterModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class IPHunter extends AbstractSource<IPHunterModel> {
    public @NonNull String getName() { return "iphunter"; }

    public boolean isKeyRequired() { return true; }

    public IPHunter() {
        super(IPHunterModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"success".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(model.getCode().contains("X-Key"), "Could not get result from " + getName() + " (" + model.getCode() + ")");
            }

            return model.getData().getBlock() == getSourceConfigNode().node("block").getInt(1);
        });
    }

    public @NonNull CompletableFuture<IPHunterModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            Map<String, String> newHeaders = new HashMap<>(headers);
            newHeaders.put("X-Key", key);

            HttpURLConnection conn = getConnection("https://www.iphunter.info:8082/v1/ip/" + ip, "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", newHeaders);
            JSONDeserializer<IPHunterModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPHunterModel.class);
        });
    }
}
