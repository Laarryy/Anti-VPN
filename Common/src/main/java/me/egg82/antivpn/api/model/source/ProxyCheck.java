package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.ProxyCheckModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class ProxyCheck extends AbstractSource<ProxyCheckModel> {
    public @NonNull String getName() { return "proxycheck"; }

    public boolean isKeyRequired() { return false; }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"ok".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(model.getMessage().contains("Key"), "Could not get result from " + getName() + " (" + model.getMessage() + ")");
            }

            ProxyCheckModel.IP ipModel;
            try {
                ipModel = (ProxyCheckModel.IP) model.getClass().getField(ip).get(model);
            } catch (NoSuchFieldException | IllegalAccessException ex) {
                throw new APIException(true, "Could not get field " + ip + " from " + model);
            }

            return "yes".equalsIgnoreCase(ipModel.getProxy());
        });
    }

    public @NonNull CompletableFuture<ProxyCheckModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();

            HttpURLConnection conn = getConnection("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : ""), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", headers);
            JSONDeserializer<ProxyCheckModel> modelDeserializer = new JSONDeserializer<>();
            modelDeserializer.use(ip, ProxyCheckModel.IP.class);
            return modelDeserializer.deserialize(getString(conn), ProxyCheckModel.class);
        });
    }
}
