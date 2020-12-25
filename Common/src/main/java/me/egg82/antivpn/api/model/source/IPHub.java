package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPHubModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class IPHub extends AbstractSource<IPHubModel> {
    public @NonNull String getName() { return "iphub"; }

    public boolean isKeyRequired() { return true; }

    public CompletableFuture<Boolean> getResult(@NonNull String ip) throws APIException {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getError() != null) {
                throw new APIException(model.getError().contains("key"), "Could not get result from " + getName() + " (" + model.getError() + ")");
            }

            return model.getBlock() == getSourceConfigNode().node("block").getInt(1);
        });
    }

    public CompletableFuture<IPHubModel> getRawResponse(@NonNull String ip) throws APIException {
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

            HttpURLConnection conn = getConnection("https://v2.api.iphub.info/ip/" + ip, "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", newHeaders);
            JSONDeserializer<IPHubModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPHubModel.class);
        });
    }
}
