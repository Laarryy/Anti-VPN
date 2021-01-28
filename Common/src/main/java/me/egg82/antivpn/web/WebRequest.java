package me.egg82.antivpn.web;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.utils.TimeUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebRequest {
    private static final Logger logger = LoggerFactory.getLogger(WebRequest.class);

    private final URL url;
    private RequestMethod method = RequestMethod.GET;
    private TimeUtil.Time timeout = new TimeUtil.Time(5L, TimeUnit.SECONDS);
    private final Map<String, String> headers = new HashMap<>();
    private final Map<String, String> postData = new HashMap<>();
    private int maxRedirects = 20;
    private boolean throwOnStandardErrors = true;

    private WebRequest(@NonNull URL url) {
        this.url = url;
    }

    public static final Map<String, String> DEFAULT_HEADERS = new HashMap<>();

    static {
        DEFAULT_HEADERS.put("Accept-Language", "en-US,en;q=0.8");
    }

    public static @NonNull String urlEncode(@NonNull String part) {
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

    public @NonNull String getString() throws IOException { return getString(getConnection()); }

    public static @NonNull String getString(@NonNull HttpURLConnection connection) throws IOException {
        try (InputStream in = getInputStream(connection); InputStreamReader reader = new InputStreamReader(in); BufferedReader buffer = new BufferedReader(reader)) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = buffer.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    public byte @NonNull [] getBytes() throws IOException { return getBytes(getConnection()); }

    public static byte @NonNull [] getBytes(@NonNull HttpURLConnection connection) throws IOException {
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

    public @NonNull InputStream getInputStream() throws IOException { return getInputStream(getConnection()); }

    public static @NonNull InputStream getInputStream(@NonNull HttpURLConnection connection) throws IOException { return connection.getInputStream(); }

    public @NonNull HttpURLConnection getConnection() throws IOException {
        if (ConfigUtil.getDebugOrFalse()) {
            logger.info("Fetching URL: " + url);
        }

        HttpURLConnection retVal = openConnection(url);
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
                retVal = openConnection(new URL(newUrl));
                setConnectionProperties(retVal, cookies);
            }
        } while (redirect);

        return retVal;
    }

    private @NonNull HttpURLConnection openConnection(@NonNull URL url) throws IOException {
        HttpURLConnection retVal = (HttpURLConnection) url.openConnection();

        if (throwOnStandardErrors) {
            int status = retVal.getResponseCode();

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

        return retVal;
    }

    private void setConnectionProperties(@NonNull HttpURLConnection conn, String cookies) throws IOException {
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout((int) timeout.getMillis());
        conn.setReadTimeout((int) timeout.getMillis());

        if (method != null) {
            conn.setRequestMethod(method.name().toLowerCase());
        }
        if (!headers.isEmpty()) {
            for (Map.Entry<String, String> kvp : headers.entrySet()) {
                conn.setRequestProperty(kvp.getKey(), kvp.getValue());
            }
        }
        if (cookies != null && !cookies.isEmpty()) {
            conn.setRequestProperty("Cookie", cookies);
        }
        if (!postData.isEmpty()) {
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
            StringBuilder data = new StringBuilder();
            for (Map.Entry<String, String> kvp : postData.entrySet()) {
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
        }
    }

    public static @NonNull Builder builder(@NonNull URL url) { return new Builder(url); }

    public static class Builder {
        private final WebRequest request;

        private Builder(@NonNull URL url) {
            request = new WebRequest(url);
        }

        public WebRequest.@NonNull Builder method(@NonNull RequestMethod value) {
            request.method = value;
            return this;
        }

        public WebRequest.@NonNull Builder timeout(TimeUtil.@NonNull Time value) {
            request.timeout = value;
            return this;
        }

        public WebRequest.@NonNull Builder userAgent(@NonNull String value) {
            request.headers.put("User-Agent", value);
            return this;
        }

        public WebRequest.@NonNull Builder header(@NonNull String key, String value) {
            if (value != null) {
                if (!hasKey(request.headers, key)) {
                    request.headers.put(key, value);
                }
            } else {
                removeKey(request.headers, key);
            }
            return this;
        }

        public WebRequest.@NonNull Builder headers(@NonNull Map<String, String> value) {
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

        public WebRequest.@NonNull Builder formData(@NonNull String key, String value) {
            if (value != null) {
                if (!hasKey(request.postData, key)) {
                    request.postData.put(key, value);
                }
            } else {
                removeKey(request.postData, key);
            }
            return this;
        }

        public WebRequest.@NonNull Builder formData(@NonNull Map<String, String> value) {
            for (Map.Entry<String, String> kvp : value.entrySet()) {
                if (kvp.getValue() != null) {
                    if (!hasKey(request.postData, kvp.getKey())) {
                        request.postData.put(kvp.getKey(), kvp.getValue());
                    }
                } else {
                    removeKey(request.postData, kvp.getKey());
                }
            }
            return this;
        }

        public WebRequest.@NonNull Builder maxRedirects(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("value cannot be negative.");
            }
            request.maxRedirects = value;
            return this;
        }

        public WebRequest.@NonNull Builder throwOnStandardErrors(boolean value) {
            request.throwOnStandardErrors = value;
            return this;
        }

        public @NonNull WebRequest build() {
            for (Map.Entry<String, String> kvp : DEFAULT_HEADERS.entrySet()) {
                if (kvp.getValue() != null && !hasKey(request.headers, kvp.getKey())) {
                    request.headers.put(kvp.getKey(), kvp.getValue());
                }
            }
            return request;
        }

        private boolean hasKey(@NonNull Map<String, String> map, @NonNull String key) {
            for (String k : map.keySet()) {
                if (k.equalsIgnoreCase(key)) {
                    return true;
                }
            }
            return false;
        }

        private void removeKey(@NonNull Map<String, String> map, @NonNull String key) {
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
        POST
    }
}
