package me.egg82.antivpn.apis.vpn;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public class Teoh extends AbstractSourceAPI {
    public String getName() { return "teoh"; }

    public boolean isKeyRequired() { return false; }

    private static AtomicInteger requests = new AtomicInteger(0);
    private static ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("AntiVPN-TeohAPI-%d").build());

    static {
        threadPool.scheduleAtFixedRate(() -> requests.set(0), 0L, 24L, TimeUnit.HOURS);
    }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        if (requests.getAndIncrement() >= 1000) {
            throw new APIException(true, "API calls to this source have been limited to 1,000/day as per request.");
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://ip.teoh.io/api/vpn/" + ip), "GET", (int) getCachedConfig().getTimeout(), "egg82/AntiVPN");
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, ex);
        }
        if (json == null || json.get("vpn_or_proxy") == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        String proxy = (String) json.get("vpn_or_proxy");
        return proxy.equalsIgnoreCase("yes");
    }
}
