package me.egg82.antivpn.api.model.source;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import flexjson.JSONDeserializer;
import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.TeohModel;
import me.egg82.antivpn.utils.ValidationUtil;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Teoh extends AbstractSource<TeohModel> {
    public @NonNull String getName() { return "teoh"; }

    public boolean isKeyRequired() { return false; }

    private static final AtomicInteger requests = new AtomicInteger(0);
    private static final ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("AntiVPN-TeohAPI-%d").build());

    static {
        threadPool.scheduleAtFixedRate(() -> requests.set(0), 0L, 24L, TimeUnit.HOURS);
    }

    public Teoh() {
        super(TeohModel.class);
    }

    public @NonNull CompletableFuture<Boolean> getResult(@NonNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getMessage() != null) {
                throw new APIException(false, "Could not get result from " + getName() + " (" + model.getMessage() + ")");
            }

            return "1".equals(model.getHosting()) || "yes".equalsIgnoreCase(model.getVpnOrProxy());
        });
    }

    public @NonNull CompletableFuture<TeohModel> getRawResponse(@NonNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            if (requests.getAndIncrement() >= 1000) {
                throw new APIException(false, "API calls to " + getName() + " have been limited to 1,000/day as per request.");
            }

            HttpURLConnection conn = getConnection("https://ip.teoh.io/api/vpn/" + ip, "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN", headers);
            JSONDeserializer<TeohModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), TeohModel.class);
        });
    }
}
