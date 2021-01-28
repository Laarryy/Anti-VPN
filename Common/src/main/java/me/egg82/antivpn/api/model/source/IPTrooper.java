package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPTrooperModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.checkerframework.checker.nullness.qual.NonNull;

public class IPTrooper extends AbstractSource<IPTrooperModel> {
    public @NonNull String getName() { return "iptrooper"; }

    public boolean isKeyRequired() { return false; }

    public IPTrooper() {
        super(IPTrooperModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getCode() == 3) {
                throw new APIException(false, "Could not get result from " + getName() + " (Request limit reached)");
            }

            return model.isBad();
        });
    }

    public @NonNull CompletableFuture<IPTrooperModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            WebRequest.Builder builder = getDefaultBuilder("https://api.iptrooper.net/check/" + ip + "?full=1", getCachedConfig().getTimeout());
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPTrooperModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPTrooperModel.class);
        });
    }
}
