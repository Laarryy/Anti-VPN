package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.ProxyCheckModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ProxyCheck extends AbstractSource<ProxyCheckModel> {
    @Override
    public @NotNull String getName() { return "proxycheck"; }

    @Override
    public boolean isKeyRequired() { return false; }

    public ProxyCheck() {
        super(ProxyCheckModel.class);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"ok".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(
                        model.getMessage() != null && model.getMessage().contains("Key"),
                        "Could not get result from " + getName() + " (" + model.getMessage() + ")"
                );
            }

            return model.getIp() != null && "yes".equalsIgnoreCase(model.getIp().getProxy());
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull ProxyCheckModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();

            WebRequest.Builder builder = getDefaultBuilder("https://proxycheck.io/v2/" + ip + "?vpn=1" + ((key != null && !key.isEmpty()) ? "&key=" + key : ""));
            HttpURLConnection conn = getConnection(builder.build());
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
