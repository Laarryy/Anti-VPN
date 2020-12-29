package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IP2ProxyModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class IP2Proxy extends AbstractSource<IP2ProxyModel> {
    public @NonNull String getName() { return "ip2proxy"; }

    public boolean isKeyRequired() { return true; }

    public IP2Proxy() {
        super(IP2ProxyModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"OK".equalsIgnoreCase(model.getResponse())) {
                throw new APIException(false, "Could not get result from " + getName() + " (" + model.getResponse() + ")");
            }

            return "YES".equalsIgnoreCase(model.getProxy());
        });
    }

    public @NonNull CompletableFuture<IP2ProxyModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            HttpURLConnection conn = getConnection("https://api.ip2proxy.com/?ip=" + ip + "&key=" + key + "&package=PX1&format=json", "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", headers);
            JSONDeserializer<IP2ProxyModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IP2ProxyModel.class);
        });
    }
}
