package me.egg82.antivpn.api.model.source;

import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.IPQualityScoreModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;

public class IPQualityScore extends AbstractSource<IPQualityScoreModel> {
    public @NotNull String getName() {
        return "ipqualityscore";
    }

    public boolean isKeyRequired() {
        return true;
    }

    public IPQualityScore() {
        super(IPQualityScoreModel.class);
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!model.isSuccess()) {
                throw new APIException(
                        model.getMessage() != null && model.getMessage().contains("key"),
                        "Could not get result from " + getName() + " (" + model.getMessage() + ")"
                );
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            if (model.isTor() || model.isVpn() || model.isBot() || model.isCrawler()) {
                return true;
            }
            if (sourceConfigNode.node("proxy").getBoolean(false) && model.isProxy()) {
                return true;
            }
            if (sourceConfigNode.node("recent-abuse").getBoolean(false) && model.isRecentAbuse()) {
                return true;
            }

            return model.getFraudScore() >= sourceConfigNode.node("threshold").getDouble(0.98d) * 100.0d;
        });
    }

    public @NotNull CompletableFuture<@NotNull IPQualityScoreModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            String key = sourceConfigNode.node("key").getString();
            if (key == null || key.isEmpty()) {
                throw new APIException(true, "Key is not defined for " + getName());
            }

            WebRequest.Builder builder = getDefaultBuilder("http://www.ipqualityscore.com/api/json/ip/" + key + "/" + ip + "?strictness=" + sourceConfigNode.node(
                    "strictness").getInt(0) + "&mobile=" + (sourceConfigNode.node("mobile").getBoolean(true)
                                                            ? "true"
                                                            : "false") + "&fast=true&allow_public_access_points=true&lighter_penalties=true");
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<IPQualityScoreModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), IPQualityScoreModel.class);
        });
    }
}
