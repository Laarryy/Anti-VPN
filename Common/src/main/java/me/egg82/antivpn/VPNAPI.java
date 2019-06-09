package me.egg82.antivpn;

import com.google.common.collect.ImmutableMap;
import java.sql.SQLException;
import java.util.Optional;
import me.egg82.antivpn.enums.SQLType;
import me.egg82.antivpn.extended.CachedConfigValues;
import me.egg82.antivpn.services.InternalAPI;
import me.egg82.antivpn.sql.MySQL;
import me.egg82.antivpn.sql.SQLite;
import me.egg82.antivpn.utils.ConfigUtil;
import me.egg82.antivpn.utils.ValidationUtil;

public class VPNAPI {
    private static final VPNAPI api = new VPNAPI();
    private final InternalAPI internalApi = new InternalAPI();

    private VPNAPI() { }

    public static VPNAPI getInstance() { return api; }

    public long getCurrentSQLTime() throws APIException {
        Optional<CachedConfigValues> cachedConfig = ConfigUtil.getCachedConfig();
        if (!cachedConfig.isPresent()) {
            throw new APIException(true, "Could not get cached config.");
        }

        try {
            if (cachedConfig.get().getSQLType() == SQLType.MySQL) {
                return MySQL.getCurrentTime();
            } else if (cachedConfig.get().getSQLType() == SQLType.SQLite) {
                return SQLite.getCurrentTime();
            }
        } catch (SQLException ex) {
            throw new APIException(true, ex);
        }

        throw new APIException(true, "Could not get time from database.");
    }

    public ImmutableMap<String, Optional<Boolean>> testAllSources(String ip) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        return ImmutableMap.copyOf(internalApi.testAllSources(ip));
    }

    public boolean getSourceResult(String ip, String sourceName) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (sourceName == null) {
            throw new IllegalArgumentException("sourceName cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        return internalApi.getSourceResult(ip, sourceName);
    }

    public double consensus(String ip) throws APIException { return consensus(ip, true); }

    public double consensus(String ip, boolean expensive) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        return internalApi.consensus(ip, expensive);
    }

    public boolean cascade(String ip) throws APIException { return cascade(ip, true); }

    public boolean cascade(String ip, boolean expensive) throws APIException {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }
        if (!ValidationUtil.isValidIp(ip)) {
            throw new IllegalArgumentException("ip is invalid.");
        }

        return internalApi.cascade(ip, expensive);
    }
}
