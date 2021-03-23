package me.egg82.antivpn.api.model.source;

import me.egg82.antivpn.api.APIException;
import me.egg82.antivpn.api.model.source.models.SourceModel;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.web.WebRequest;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

public abstract class AbstractSource<T extends SourceModel> implements Source<T> {
    private final @NotNull Class<T> modelClass;

    protected AbstractSource(@NotNull Class<T> modelClass) {
        this.modelClass = modelClass;
    }

    @Override
    @NotNull
    public Class<T> getModelClass() { return modelClass; }

    @NotNull
    protected final ConfigurationNode getSourceConfigNode() { return ConfigUtil.getConfig().node("sources", getName()); }

    @NotNull
    protected final WebRequest.Builder getDefaultBuilder(@NotNull String url) throws APIException {
        try {
            return WebRequest.builder(new URL(url))
                    .timeout(new TimeUtil.Time(ConfigUtil.getCachedConfig().getTimeout(), TimeUnit.MILLISECONDS))
                    .userAgent("egg82/Anti-VPN")
                    .header("Accept", "application/json")
                    .throwOnStandardErrors(false);
        } catch (IOException ex) {
            throw new APIException(false, "Could not get builder for " + getName(), ex);
        }
    }

    @NotNull
    protected final HttpURLConnection getConnection(@NotNull WebRequest request) throws APIException {
        try {
            HttpURLConnection conn = request.getConnection();
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

    @NotNull
    protected final String getString(HttpURLConnection conn) throws APIException {
        try {
            return WebRequest.getString(conn);
        } catch (IOException ex) {
            throw new APIException(false, "Could not get result from " + getName(), ex);
        }
    }
}
