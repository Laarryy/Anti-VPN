package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.ProxyCheckModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class ProxyCheck extends AbstractSource<ProxyCheckModel> {
    public @NonNull String getName() { return "proxycheck"; }

    public boolean isKeyRequired() { return false; }

    public ProxyCheck() {
        super(ProxyCheckModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"ok".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(model.getMessage().contains("Key"), "Could not get result from " + getName() + " (" + model.getMessage() + ")");
            }

            return "yes".equalsIgnoreCase(model.getIp().getProxy());
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
            String str = getString(conn);

            JSONDeserializer<Map<String, Object>> mapDeserializer = new JSONDeserializer<>();
            Map<String, Object> map = mapDeserializer.deserialize(str);
            ProxyCheckModel.IP ipModel = null;
            if (map.containsKey(ip)) {
                JSONDeserializer<ProxyCheckModel.IP> ipModelDeserializer = new JSONDeserializer<>();
                ipModel = ipModelDeserializer.deserialize(new JSONSerializer().exclude("*.class").deepSerialize(map.get(ip)), ProxyCheckModel.IP.class);
            }

            JSONDeserializer<ProxyCheckModel> modelDeserializer = new JSONDeserializer<>();
            ProxyCheckModel model = modelDeserializer.deserialize(str, ProxyCheckModel.class);
            model.setIp(ipModel);
            return model;
        });
    }
}
