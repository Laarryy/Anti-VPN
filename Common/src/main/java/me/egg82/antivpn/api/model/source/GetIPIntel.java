package me.egg82.antivpn.api.model.source;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.GetIPIntelModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GetIPIntel extends AbstractSource<GetIPIntelModel> {
    private static final AtomicInteger hourlyRequests = new AtomicInteger(0);
    private static final AtomicInteger minuteRequests = new AtomicInteger(0);
    private static final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(
            "Anti-VPN_GetIPIntel_%d").build());

    static {
        threadPool.scheduleAtFixedRate(() -> hourlyRequests.set(0), 0L, 24L, TimeUnit.HOURS);
        threadPool.scheduleAtFixedRate(() -> minuteRequests.set(0), 0L, 1L, TimeUnit.MINUTES);
    }

    public GetIPIntel() {
        super(GetIPIntelModel.class);
    }

    public @NotNull String getName() {
        return "getipintel";
    }

    public boolean isKeyRequired() {
        return false;
    }

    public @NotNull CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (!"success".equalsIgnoreCase(model.getStatus()) || model.getResult() == null) {
                boolean isHard = model.getResult() != null && ("-5".equals(model.getResult()) || "-6".equals(model.getResult()));
                throw new APIException(
                        isHard,
                        "Could not get result from " + getName() + " (" + model.getResult() + ": " + model.getMessage() + ")" + (isHard
                                                                                                                                 ? " (Is your server's IP banned due to an improper contact e-mail in the config? Send an e-mail to contact@getipintel.net for an unban)"
                                                                                                                                 : "")
                );
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();

            double retVal = Double.parseDouble(model.getResult());
            return retVal >= sourceConfigNode.node("threshold").getDouble(0.98);
        });
    }

    public @NotNull CompletableFuture<@NotNull GetIPIntelModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            ConfigurationNode sourceConfigNode = getSourceConfigNode();
            if ("admin@yoursite.com".equalsIgnoreCase(sourceConfigNode.node("contact").getString("admin@yoursite.com"))) {
                throw new APIException(
                        true,
                        "Contact is not defined for " + getName() + " (WARNING: USING AN INVALID E-MAIL FOR THE CONTACT WILL GET YOUR IP BANNED FROM THE SERVICE)"
                );
            }

            if (hourlyRequests.getAndIncrement() >= 500) {
                throw new APIException(false, "API calls to this source have been limited to 500/day as per request.");
            }
            if (minuteRequests.getAndIncrement() >= 15) {
                throw new APIException(false, "API calls to this source have been limited to 15/minute as per request.");
            }

            WebRequest.Builder builder = getDefaultBuilder("https://" + sourceConfigNode.node("subdomain")
                    .getString("check") + ".getipintel.net/check.php?ip=" + ip + "&contact=" + sourceConfigNode.node("contact")
                    .getString("admin@yoursite.com") + "&format=json&flags=b");
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<GetIPIntelModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), GetIPIntelModel.class);
        });
    }
}
