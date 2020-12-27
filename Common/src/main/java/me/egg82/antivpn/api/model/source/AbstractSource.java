package me.egg82.antivpn.api.model.source;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.CachedConfig;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.WebUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.ConfigurationNode;

public abstract class AbstractSource<T extends SourceModel> implements Source<T> {
    protected static final Map<String, String> headers = new HashMap<>();
    static {
        headers.put("Accept", "application/json");
        headers.put("Connection", "close");
        headers.put("Accept-Language", "en-US,en;q=0.8");
    }

    protected final @NonNull ConfigurationNode getSourceConfigNode() throws APIException {
        ConfigurationNode config = ConfigUtil.getConfig();
        if (config == null) {
            throw new APIException(false, "Could not get configuration.");
        }

        return config.node("sources", getName());
    }

    protected final @NonNull CachedConfig getCachedConfig() throws APIException {
        CachedConfig cachedConfig = ConfigUtil.getCachedConfig();
        if (cachedConfig == null) {
            throw new APIException(false, "Cached config could not be fetched.");
        }

        return cachedConfig;
    }

    protected final @NonNull HttpURLConnection getConnection(String url, String method, int timeout, String userAgent, Map<String, String> headers) throws APIException { return getConnection(url, method, timeout, userAgent, headers, null); }

    protected final @NonNull HttpURLConnection getConnection(String url, String method, int timeout, String userAgent, Map<String, String> headers, Map<String, String> postData) throws APIException {
        try {
            HttpURLConnection conn = WebUtil.getConnection(new URL(url), method, timeout, userAgent, headers, postData);
            int status = conn.getResponseCode();

            if (status >= 200 && status < 300) {
                if (status == 204 || status == 205) { // Empty response, reset
                    throw new APIException(false, "Could not get result from " + getName() + " (HTTP status " + status + " - empty response/reset connection)");
                }

                return conn;
            } else if (status >= 400 && status < 500) {
                if (status == 401 || status == 403) { // Unauthorized, forbidden
                    throw new APIException(true, "Could not get result from " + getName() + " (HTTP status " + status + " - access denied, key/token issue)");
                }
                if (status == 429) { // Too many queries
                    throw new APIException(false, "Could not get result from " + getName() + " (HTTP status " + status + " - too many queries, temporary issue)");
                }
                throw new APIException(true, "Could not get result from " + getName() + " (HTTP status " + status + ")");
            } else if (status >= 500 && status < 600) { // Server errors (usually temporary)
                throw new APIException(false, "Could not get result from " + getName() + " (HTTP status " + status + ")");
            }
            throw new APIException(true, "Could not get result from " + getName() + " (HTTP status " + status + ")");
        } catch (IOException ex) {
            throw new APIException(false, "Could not get result from " + getName(), ex);
        }
    }

    protected final @NonNull String getString(HttpURLConnection conn) throws APIException {
        try {
            return WebUtil.getString(conn);
        } catch (IOException ex) {
            throw new APIException(false, "Could not get result from " + getName(), ex);
        }
    }
}
