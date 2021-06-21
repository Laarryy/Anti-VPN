package me.egg82.antivpn.api.model.source;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import flexjson.JSONDeserializer;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.TeohModel;
import me.egg82.antivpn.utils.ValidationUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;

import java.net.HttpURLConnection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Teoh extends AbstractSource<TeohModel> {
    @Override
    @NotNull
    public String getName() { return "teoh"; }

    @Override
    public boolean isKeyRequired() { return false; }

    private static final @NotNull AtomicInteger requests = new AtomicInteger(0);
    private static final @NotNull ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Anti-VPN_TeohAPI_%d").build());

    static {
        threadPool.scheduleAtFixedRate(() -> requests.set(0), 0L, 24L, TimeUnit.HOURS);
    }

    public Teoh() {
        super(TeohModel.class);
    }

    @Override
    @NotNull
    public CompletableFuture<@NotNull Boolean> getResult(@NotNull String ip) {
        return getRawResponse(ip).thenApply(model -> {
            if (model.getMessage() != null) {
                throw new APIException(false, "Could not get result from " + getName() + " (" + model.getMessage() + ")");
            }

            return model.getHosting() == 1 || "yes".equalsIgnoreCase(model.getVpnOrProxy());
        });
    }

    @Override
    @NotNull
    public CompletableFuture<@NotNull TeohModel> getRawResponse(@NotNull String ip) {
        return CompletableFuture.supplyAsync(() -> {
            if (!ValidationUtil.isValidIp(ip)) {
                throw new IllegalArgumentException("ip is invalid.");
            }

            if (requests.getAndIncrement() >= 1000) {
                throw new APIException(false, "API calls to " + getName() + " have been limited to 1,000/day as per request.");
            }

            WebRequest.Builder builder = getDefaultBuilder("https://vpnapi.io/api/" + ip);
            HttpURLConnection conn = getConnection(builder.build());
            JSONDeserializer<TeohModel> modelDeserializer = new JSONDeserializer<>();
            return modelDeserializer.deserialize(getString(conn), TeohModel.class);
        });
    }
}
