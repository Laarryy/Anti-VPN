package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPInfoModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class IPInfo extends AbstractSource<IPInfoModel> {
    public @NonNull String getName() { return "ipinfo"; }

    public boolean isKeyRequired() { return true; }

    public IPInfo() {
        super(IPInfoModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getError() != null) {
                throw new APIException(model.getError().getMessage().contains("token"), "Could not get result from " + getName() + " (" + model.getError().getMessage() + ")");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            // if proxy config setting is true and "proxy" is true, tor || vpn will also be true.
            if (sourceConfigNode.node("proxy").getBoolean(true) && model.isProxy()) {
                return true;
            }
            return model.isTor() || model.isVpn();
        });
    }

    public @NonNull CompletableFuture<IPInfoModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("https://ipinfo.io/" + ip + "/privacy?token=" + key, getCachedConfig().getTimeout());
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPInfoModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPInfoModel.class);
        });
    }
}
