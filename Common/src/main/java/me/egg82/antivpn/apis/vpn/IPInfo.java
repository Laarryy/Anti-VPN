package me.egg82.antivpn.apis.vpn;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.egg82.antivpn.APIException;
import me.egg82.antivpn.utils.ValidationUtil;
import ninja.egg82.json.JSONWebUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IPInfo extends AbstractSourceAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static AtomicInteger hourlyRequests = new AtomicInteger(0);
    private static AtomicInteger minuteRequests = new AtomicInteger(0);
    private static ScheduledExecutorService threadPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("AntiVPN-IPInfoAPI-%d").build());

    static {
        threadPool.scheduleAtFixedRate(() -> hourlyRequests.set(0), 0L, 24L, TimeUnit.HOURS);
        threadPool.scheduleAtFixedRate(() -> minuteRequests.set(0), 0L, 1L, TimeUnit.MINUTES);
    }

    public String getName() { return "ipinfo"; }

    public boolean isKeyRequired() { return true; }

    public boolean getResult(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        ConfigurationNode sourceConfigNode = getSourceConfigNode();

        String key = sourceConfigNode.getNode("key").getString();
        if (key == null || key.isEmpty()) {
            throw new APIException(true, "Key is not defined for " + getName());
        }

        if (hourlyRequests.getAndIncrement() >= 500) {
            throw new APIException(true, "API calls to this source have been limited to 500/day as per request.");
        }
        if (minuteRequests.getAndIncrement() >= 15) {
            throw new APIException(true, "API calls to this source have been limited to 15/minute as per request.");
        }

        JSONObject json;
        try {
            json = JSONWebUtil.getJSONObject(new URL("https://ipinfo.io/" + ip + "/privacy?token=" + key), "GET", (int) getCachedConfig().getTimeout());
            logger.info(String.valueOf(json));
        } catch (IOException | ParseException | ClassCastException ex) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json == null) {
            throw new APIException(false, "Could not get result from " + getName());
        }
        if (json.isEmpty()) {
            throw new APIException(false, "Could not get result from " + getName());
        }

        Boolean vpnStatus = (Boolean) json.get("vpn");
        Boolean proxyStatus = (Boolean) json.get("proxy");

        String proxy = sourceConfigNode.getNode("proxy").getString();
        if (proxy == null || proxy.isEmpty()) {
            throw new APIException(true, "proxy setting is invalid");
        }
        // if proxy config setting is true and "proxy" is true, tor || vpn will also be true.
        if (sourceConfigNode.getNode("proxy").getBoolean() && json.get("proxy") != null) {
            if (proxyStatus) {
                return true;
            } else return vpnStatus;
        }
        return vpnStatus;
    }
}
