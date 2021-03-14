package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPTrooperModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class IPTrooper extends AbstractSource<IPTrooperModel> {
    public @NotNull String getName() {
        return "iptrooper";
    }

    public boolean isKeyRequired() {
        return false;
    }

    public IPTrooper() {
        super(IPTrooperModel.class);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getCode() == 3) {
                throw new APIException(false, "Could not get result from " + getName() + " (Request limit reached)");
            }

            return model.isBad();
        });
    }

    public @NotNull CompletableFuture<@NotNull IPTrooperModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            WebRequest.Builder builder = getDefaultBuilder("https://api.iptrooper.net/check/" + ip + "?full=1");
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPTrooperModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPTrooperModel.class);
        });
    }
}
