package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IP2ProxyModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

public class IP2Proxy extends AbstractSource<IP2ProxyModel> {
    public @NotNull String getName() { return "ip2proxy"; }

    public boolean isKeyRequired() { return true; }

    public IP2Proxy() {
        super(IP2ProxyModel.class);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"OK".equalsIgnoreCase(model.getResponse())) {
                throw new APIException(false, "Could not get result from " + getName() + " (" + model.getResponse() + ")");
            }

            return "YES".equalsIgnoreCase(model.getProxy());
        });
    }

    public @NotNull CompletableFuture<@NotNull IP2ProxyModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("https://api.ip2proxy.com/?ip=" + ip + "&key=" + key + "&package=PX1&format=json");
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IP2ProxyModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IP2ProxyModel.class);
        });
    }
}
