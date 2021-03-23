package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPHubModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class IPHub extends AbstractSource<IPHubModel> {
    @Override
    @NotNull
    public String getName() { return "iphub"; }

    @Override
    public boolean isKeyRequired() { return true; }

    public IPHub() {
        super(IPHubModel.class);
    }

    @Override
    @NotNull
    public CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getError() != null) {
                throw new APIException(model.getError().contains("key"), "Could not get result from " + getName() + " (" + model.getError() + ")");
            }

            return model.getBlock() == getSourceConfigNode().node("block").getInt(1);
        });
    }

    @Override
    @NotNull
    public CompletableFuture<@NotNull IPHubModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("https://v2.api.iphub.info/ip/" + ip);
            builder.header("X-Key", key);
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPHubModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPHubModel.class);
        });
    }
}
