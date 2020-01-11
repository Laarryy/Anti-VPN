package me.egg82.antivpn;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import me.egg82.antivpn.utils.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VPNAPI {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final VPNAPI api = new VPNAPI();

    private final AtomicLong numSentMessages = new AtomicLong(0L);

    private VPNAPI() { }

    public static VPNAPI getInstance() { return api; }

    public ImmutableMap<String, Optional<Boolean>> testAllSources(String ip) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        return ImmutableMap.copyOf(internalApi.testAllSources(ip));
    }

    public boolean getSourceResult(String ip, String sourceName) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }
        if (sourceName == null) {
            throw new APIException(false, "sourceName cannot be null.");
        }

        return internalApi.getSourceResult(ip, sourceName);
    }

    public double consensus(String ip) throws APIException { return consensus(ip, true); }

    public double consensus(String ip, boolean expensive) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        return internalApi.consensus(ip, expensive);
    }

    public boolean cascade(String ip) throws APIException { return cascade(ip, true); }

    public boolean cascade(String ip, boolean expensive) throws APIException {
        if (ip == null) {
            throw new APIException(false, "ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new APIException(false, "ip is invalid.");
        }

        return internalApi.cascade(ip, expensive);
    }
}
