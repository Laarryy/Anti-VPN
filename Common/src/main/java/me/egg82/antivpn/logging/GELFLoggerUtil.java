package me.egg82.antivpn.logging;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import flexjson.JSONSerializer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.egg82.antivpn.api.platform.Platform;
import me.egg82.antivpn.compress.GZIPCompressionStream;
import me.egg82.antivpn.core.DoubleBuffer;
import me.egg82.antivpn.locale.LocaleUtil;
import me.egg82.antivpn.locale.MessageKey;
import me.egg82.antivpn.logging.models.GELFSubmissionModel;
import me.egg82.antivpn.utils.TimeUtil;
import me.egg82.antivpn.web.WebRequest;
import me.egg82.antivpn.web.transformers.InstantTransformer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GELFLoggerUtil {
    private static final Logger logger = LoggerFactory.getLogger(GELFLoggerUtil.class);

    private static final ScheduledExecutorService workPool = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat("Anti-VPN_GELFLoggerUtil_%d").build());

    static {
        workPool.scheduleWithFixedDelay(GELFLoggerUtil::sendModels, 1L, 2L, TimeUnit.SECONDS);
    }

    private GELFLoggerUtil() { }

    private static final GZIPCompressionStream GZIP_COMPRESSION = new GZIPCompressionStream();

    private static final String GELF_ADDRESS = "https://logs.egg82.me:8443/gelf";

    private static final String SESSION_ID = UUID.randomUUID().toString();

    private static String serverId = null;
    private static String pluginVersion = null;
    private static String platform = null;
    private static String platformVersion = null;

    private static volatile boolean initialized = false;
    private static volatile boolean sendErrors = false;

    private static final DoubleBuffer<GELFSubmissionModel> modelQueue = new DoubleBuffer<>();

    public static void setData(@NotNull UUID serverId, @NotNull String pluginVersion, @NotNull Platform.Type platform, @NotNull String platformVersion) {
        GELFLoggerUtil.serverId = serverId.toString();
        GELFLoggerUtil.pluginVersion = pluginVersion;
        GELFLoggerUtil.platform = platform.getFriendlyName();
        GELFLoggerUtil.platformVersion = platformVersion;
    }

    public static void doSendErrors(boolean sendErrors) {
        GELFLoggerUtil.initialized = true;
        GELFLoggerUtil.sendErrors = sendErrors;
        if (!sendErrors) {
            modelQueue.getReadBuffer().clear();
            modelQueue.getWriteBuffer().clear();
        }
    }

    public static void close() {
        workPool.shutdown();
        try {
            if (!workPool.awaitTermination(4L, TimeUnit.SECONDS)) {
                workPool.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /*public static @NotNull Pair<String, Throwable> getBuriedException(@NotNull Throwable ex) {
        Throwable oldEx = null;
        if (ex instanceof CompletionException || ex instanceof ExecutionException) {
            oldEx = ex;
            ex = ex.getCause();
        }
        while (ex instanceof CompletionException || ex instanceof ExecutionException) {
            ex = ex.getCause();
        }

        if (ex instanceof APIException) {
            String apiError = LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__API_EXCEPTION, "{hard}", String.valueOf(((APIException) ex).isHard()), "{message}", ex.getClass().getName() + ": " + ex.getLocalizedMessage());
            if (ConfigUtil.getDebugOrFalse()) {
                return new Pair<>(apiError, oldEx != null ? oldEx : ex);
            } else {
                return new Pair<>(apiError, null);
            }
        } else {
            if (ConfigUtil.getDebugOrFalse()) {
                return new Pair<>(null, oldEx != null ? oldEx : ex);
            } else {
                return new Pair<>(null, null);
            }
        }
    }*/

    public static void send(int level, @Nullable String message) {
        sendModel(getModel(level, message));
    }

    public static void queue(int level, @Nullable String message) {
        if (!initialized || sendErrors) {
            modelQueue.getWriteBuffer().add(getModel(level, message));
        }
    }

    public static void send(int level, @Nullable String message, @NotNull Throwable ex) { sendModel(getModel(level, message, ex)); }

    public static void queue(int level, @Nullable String message, @NotNull Throwable ex) {
        if (!initialized || sendErrors) {
            modelQueue.getWriteBuffer().add(getModel(level, message, ex));
        }
    }

    private static GELFSubmissionModel getModel(int level, @Nullable String message) {
        GELFSubmissionModel retVal = new GELFSubmissionModel();
        retVal.setHost(serverId);
        retVal.setShortMessage(message != null ? message : "null");
        retVal.setLevel(level);
        retVal.setSession(SESSION_ID);
        retVal.setPluginVersion(pluginVersion);
        retVal.setPlatform(platform);
        retVal.setPlatformVersion(platformVersion);
        return retVal;
    }

    private static GELFSubmissionModel getModel(int level, @Nullable String message, @NotNull Throwable ex) {
        GELFSubmissionModel retVal = new GELFSubmissionModel();
        retVal.setHost(serverId);
        retVal.setShortMessage(message != null ? message : "null");
        try (StringWriter builder = new StringWriter(); PrintWriter writer = new PrintWriter(builder)) {
            ex.printStackTrace(writer);
            String str = builder.toString();
            retVal.setFullMessage(str.substring(0, str.length() - System.lineSeparator().length()));
        } catch (IOException ex2) {
            logger.error(ex2.getClass().getName() + ": " + ex2.getLocalizedMessage(), ex2);
        }
        retVal.setLevel(level);
        retVal.setSession(SESSION_ID);
        retVal.setPluginVersion(pluginVersion);
        retVal.setPlatform(platform);
        retVal.setPlatformVersion(platformVersion);
        return retVal;
    }

    private static void sendModels() {
        if (!initialized || !sendErrors) {
            return;
        }

        modelQueue.swapBuffers();

        GELFSubmissionModel model;
        while ((model = modelQueue.getReadBuffer().poll()) != null) {
            sendModel(model);
        }
    }

    private static void sendModel(@NotNull GELFSubmissionModel model) {
        JSONSerializer modelSerializer = new JSONSerializer();
        modelSerializer.prettyPrint(false);
        modelSerializer.transform(new InstantTransformer(), Instant.class);

        try {
            WebRequest request = WebRequest.builder(new URL(GELF_ADDRESS))
                .method(WebRequest.RequestMethod.POST)
                .timeout(new TimeUtil.Time(5000L, TimeUnit.MILLISECONDS))
                .userAgent("egg82/GELFLogger")
                .header("Content-Type", "application/json")
                .header("Content-Encoding", "gzip")
                .outputData(GZIP_COMPRESSION.compress(modelSerializer.exclude("*.class").deepSerialize(model).getBytes(StandardCharsets.UTF_8)))
                .build();
            HttpURLConnection conn = request.getConnection();
            if (conn.getResponseCode() != 202) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__LOGGER__SEND, "{code}", String.valueOf(conn.getResponseCode()), "{message}", WebRequest.getString(conn)));
            }
        } catch (IOException ex) {
            logger.error(ex.getClass().getName() + ": " + ex.getLocalizedMessage(), ex);
        }
    }
}
