package me.egg82.antivpn.web;

import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.logging.GELFLogger;
import me.egg82.antivpn.utils.TimeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WebRequest {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(WebRequest.class));

    private final URL url;
    private RequestMethod method = RequestMethod.GET;
    private TimeUtil.Time timeout = new TimeUtil.Time(5L, TimeUnit.SECONDS);
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> formData = new HashMap<>();
    private byte[] outputData = null;
    private int maxRedirects = 20;
    private boolean throwOnStandardErrors = true;
    private Proxy proxy = null;

    private WebRequest(@NotNull URL url) {
        this.url = url;
    }

    public static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();

    static {
        DEFAULT_HEADERS.put("Accept-Language", "en-US,en;q=0.8");
    }

    public static @NotNull String urlEncode(@NotNull String part) {
        try {
            return URLEncoder.encode(part, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ignored) {
            try {
                return URLEncoder.encode(part, StandardCharsets.US_ASCII.toString());
            } catch (UnsupportedEncodingException ignored2) {
                return part;
            }
        }
    }

    public @NotNull String getString() throws IOException { return getString(getConnection()); }

    public static @NotNull String getString(@NotNull HttpURLConnection connection) throws IOException {
        String sep = System.lineSeparator();

        try (InputStream in = getInputStream(connection); InputStreamReader reader = new InputStreamReader(in); BufferedReader buffer = new BufferedReader(reader)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null) {
                builder.append(line);
                builder.append(sep);
            }
            if (builder.length() > 0) {
                builder.delete(builder.length() - sep.length(), builder.length());
            }
            return builder.toString();
        }
    }

    public byte @NotNull [] getBytes() throws IOException { return getBytes(getConnection()); }

    public static byte @NotNull [] getBytes(@NotNull HttpURLConnection connection) throws IOException {
        try (InputStream in = getInputStream(connection); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int read;
            byte[] buffer = new byte[4096];
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
            return out.toByteArray();
        }
    }

    public @NotNull InputStream getInputStream() throws IOException { return getInputStream(getConnection()); }

    public static @NotNull InputStream getInputStream(@NotNull HttpURLConnection connection) throws IOException { return connection.getInputStream(); }

    public @NotNull HttpURLConnection getConnection() throws IOException {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Fetching URL: " + url);
        }

        HttpURLConnection retVal = (HttpURLConnection) (proxy != null ? url.openConnection(proxy) : url.openConnection());
        setConnectionProperties(retVal, null);

        Set<String> previousUrls = new HashSet<>();
        previousUrls.add(url.toExternalForm() + (headers.containsKey("Set-Cookie") ? ":" + headers.get("Set-Cookie") : ""));

        int status;
        boolean redirect;
        do {
            status = retVal.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String cookies = retVal.getHeaderField("Set-Cookie");
                String newUrl = retVal.getHeaderField("Location");
                if (newUrl.charAt(0) == '/') {
                    newUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), newUrl, null).toExternalForm();
                }
                if (!previousUrls.add(newUrl + (cookies != null ? ":" + cookies : ""))) {
                    throw new IOException("Recursive redirect detected.");
                }
                if (maxRedirects >= 0 && previousUrls.size() > maxRedirects) {
                    throw new IOException("Too many redirects.");
                }
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Redirected to URL: " + newUrl);
                }
                retVal = (HttpURLConnection) (proxy != null ? new URL(newUrl).openConnection(proxy) : new URL(newUrl).openConnection());
                setConnectionProperties(retVal, cookies);
            }
        } while (redirect);

        tryThrowOnStandardErrors(retVal);

        return retVal;
    }

    private void tryThrowOnStandardErrors(@NotNull HttpURLConnection connection) throws IOException {
        if (throwOnStandardErrors) {
            int status = connection.getResponseCode();

            if (status >= 200 && status < 300) {
                if (status == HttpURLConnection.HTTP_RESET) {
                    throw new IOException("Could not get connection (HTTP status " + status + " - reset connection)");
                }
            } else if (status >= 300 && status < 400) {
                if (status != HttpURLConnection.HTTP_MOVED_TEMP && status != HttpURLConnection.HTTP_MOVED_PERM && status != HttpURLConnection.HTTP_SEE_OTHER) {
                    throw new IOException("Could not get connection (HTTP status " + status + " - reset connection)");
                }
            } else if (status >= 400 && status < 500) {
                if (status == HttpURLConnection.HTTP_UNAUTHORIZED || status == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new IOException("Could not get connection (HTTP status " + status + " - access denied)");
                }
                if (status == 429) { // Too many queries
                    throw new IOException("Could not get connection (HTTP status " + status + " - too many queries, temporary issue)");
                }
                throw new IOException("Could not get connection (HTTP status " + status + ")");
            } else if (status >= 500 && status < 600) { // Server errors (usually temporary)
                throw new IOException("Could not get connection (HTTP status " + status + ")");
            } else {
                throw new IOException("Could not get connection (HTTP status " + status + ")");
            }
        }
    }

    private void setConnectionProperties(@NotNull HttpURLConnection conn, String cookies) throws IOException {
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout((int) timeout.getMillis());
        conn.setReadTimeout((int) timeout.getMillis());

        if (method != RequestMethod.GET) {
            conn.setRequestMethod(method.name());
        }
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> kvp : headers.entrySet()) {
                conn.setRequestProperty(kvp.getKey(), kvp.getValue());
            }
        }
        if (cookies != null && !cookies.isEmpty()) {
            conn.setRequestProperty("Cookie", cookies);
        }

        if (!formData.isEmpty()) {
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            StringBuilder data = new StringBuilder();
            for (Map.Entry<String, String> kvp : formData.entrySet()) {
                data.append(URLEncoder.encode(kvp.getKey(), StandardCharsets.UTF_8.name()));
                data.append('=');
                data.append(URLEncoder.encode(kvp.getValue(), StandardCharsets.UTF_8.name()));
                data.append('&');
            }
            if (data.length() > 0) {
                data.deleteCharAt(data.length() - 1);
            }
            byte[] dataBytes = data.toString().getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(dataBytes.length));
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(dataBytes);
            }
        } else if (outputData != null) {
            conn.setRequestProperty("Content-Length", String.valueOf(outputData.length));
            conn.setDoOutput(true);
            try (OutputStream out = conn.getOutputStream()) {
                out.write(outputData);
            }
        }
    }

    public static @NotNull WebRequest.Builder builder(@NotNull URL url) { return new WebRequest.Builder(url); }

    public static class Builder {
        private final WebRequest request;

        private Builder(@NotNull URL url) {
            request = new WebRequest(url);
        }

        public @NotNull WebRequest.@NotNull Builder method(@NotNull RequestMethod value) {
            request.method = value;
            return this;
        }

        public @NotNull WebRequest.@NotNull Builder timeout(@NotNull TimeUtil.Time value) {
            request.timeout = value;
            return this;
        }

        public @NotNull WebRequest.Builder userAgent(@NotNull String value) {
            request.headers.put("User-Agent", value);
            return this;
        }

        public @NotNull WebRequest.Builder header(@NotNull String key, @Nullable String value) {
            if (value != null) {
                if (!hasKey(request.headers, key)) {
                    request.headers.put(key, value);
                }
            } else {
                removeKey(request.headers, key);
            }
            return this;
        }

        public @NotNull WebRequest.Builder headers(@NotNull Map<String, String> value) {
            for (Map.Entry<String, String> kvp : value.entrySet()) {
                if (kvp.getValue() != null) {
                    if (!hasKey(request.headers, kvp.getKey())) {
                        request.headers.put(kvp.getKey(), kvp.getValue());
                    }
                } else {
                    removeKey(request.headers, kvp.getKey());
                }
            }
            return this;
        }

        public @NotNull WebRequest.Builder formData(@NotNull String key, @Nullable String value) {
            if (request.outputData != null) {
                throw new IllegalStateException("Cannot add form data when output data is set.");
            }

            if (value != null) {
                if (!hasKey(request.formData, key)) {
                    request.formData.put(key, value);
                }
            } else {
                removeKey(request.formData, key);
            }
            return this;
        }

        public @NotNull WebRequest.Builder formData(@NotNull Map<String, String> value) {
            if (request.outputData != null) {
                throw new IllegalStateException("Cannot add form data when output data is set.");
            }

            for (Map.Entry<String, String> kvp : value.entrySet()) {
                if (kvp.getValue() != null) {
                    if (!hasKey(request.formData, kvp.getKey())) {
                        request.formData.put(kvp.getKey(), kvp.getValue());
                    }
                } else {
                    removeKey(request.formData, kvp.getKey());
                }
            }
            return this;
        }

        public @NotNull WebRequest.Builder outputData(byte @Nullable [] value) {
            if (value != null && !request.formData.isEmpty()) {
                throw new IllegalStateException("Cannot add output data when form data is set.");
            }

            request.outputData = value;
            return this;
        }

        public @NotNull WebRequest.Builder maxRedirects(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be negative.");
            }
            request.maxRedirects = value;
            return this;
        }

        public @NotNull WebRequest.Builder throwOnStandardErrors(boolean value) {
            request.throwOnStandardErrors = value;
            return this;
        }

        public @NotNull WebRequest.Builder proxy(@Nullable Proxy value) {
            request.proxy = value;
            return this;
        }

        public @NotNull WebRequest build() {
            for (Map.Entry<String, String> kvp : DEFAULT_HEADERS.entrySet()) {
                if (kvp.getValue() != null && !hasKey(request.headers, kvp.getKey())) {
                    request.headers.put(kvp.getKey(), kvp.getValue());
                }
            }

            return request;
        }

        private boolean hasKey(@NotNull Map<String, String> map, @NotNull String key) {
            for (String k : map.keySet()) {
                if (k.equalsIgnoreCase(key)) {
                    return true;
                }
            }
            return false;
        }

        private void removeKey(@NotNull Map<String, String> map, @NotNull String key) {
            String newKey = null;
            do {
                for (String k : map.keySet()) {
                    if (k.equalsIgnoreCase(key)) {
                        newKey = k;
                        break;
                    }
                }
                if (newKey != null) {
                    map.remove(newKey);
                }
            } while (newKey != null);
        }
    }

    public enum RequestMethod {
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        CONNECT,
        OPTIONS,
        TRACE,
        PATCH
    }
}
