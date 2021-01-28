package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.VPNBlockerModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public class VPNBlocker extends AbstractSource<VPNBlockerModel> {
    public @NonNull String getName() { return "vpnblocker"; }

    public boolean isKeyRequired() { return false; }

    public VPNBlocker() {
        super(VPNBlockerModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"success".equalsIgnoreCase(model.getStatus())) {
                throw new APIException(model.getMsg().contains("key"), "Could not get result from " + getName() + " (" + model.getMsg() + ")");
            }

            return model.isHost();
        });
    }

    public @NonNull CompletableFuture<VPNBlockerModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();

            WebRequest.Builder builder = getDefaultBuilder("http" + ((key != null && !key.isEmpty()) ? "s" : "") + "://api.vpnblocker.net/v2/json/" + ip + ((key != null && !key.isEmpty()) ? "/" + key : ""), getCachedConfig().getTimeout());
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<VPNBlockerModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), VPNBlockerModel.class);
        });
    }
}
