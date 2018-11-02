package me.egg82.antivpn.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.List;
import me.lucko.jarrelocator.JarRelocator;
import me.lucko.jarrelocator.Relocation;

/**
 * Some of this code taken from LuckPerms
 * https://github.com/lucko/LuckPerms/blob/55220e9d104de7a9405237bdd8624a781ac23109/common/src/main/java/me/lucko/luckperms/common/dependencies/classloader/ReflectionClassLoader.java
 */

public class JarUtil {
    private static final Method ADD_URL_METHOD;

    static {
        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            ADD_URL_METHOD.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private JarUtil() {}

    public static void loadJar(String url, File input, URLClassLoader classLoader) throws IOException, IllegalAccessException, InvocationTargetException {
        if (input.exists() && input.isDirectory()) {
            Files.delete(input.toPath());
        }

        if (!input.exists()) {
            downloadJar(url, input);
        }

        ADD_URL_METHOD.invoke(classLoader, input.toPath().toUri().toURL());
    }

    public static void loadJar(String url, File input, File output, URLClassLoader classLoader, List<Relocation> rules) throws IOException, IllegalAccessException, InvocationTargetException {
        if (input.exists() && input.isDirectory()) {
            Files.delete(input.toPath());
        }
        if (output.exists() && output.isDirectory()) {
            Files.delete(output.toPath());
        }

        if (!input.exists()) {
            downloadJar(url, input);
        }

        if (!output.exists()) {
            new JarRelocator(input, output, rules).run();
        }

        ADD_URL_METHOD.invoke(classLoader, output.toPath().toUri().toURL());
    }

    private static void downloadJar(String url, File output) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
        conn.setInstanceFollowRedirects(true);

        boolean redirect;

        do {
            int status = conn.getResponseCode();
            redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

            if (redirect) {
                String newUrl = conn.getHeaderField("Location");
                String cookies = conn.getHeaderField("Set-Cookie");

                conn = (HttpURLConnection) new URL(newUrl).openConnection();
                conn.setRequestProperty("Cookie", cookies);
                conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            }
        } while (redirect);

        try (BufferedInputStream in = new BufferedInputStream(conn.getInputStream()); FileOutputStream fileOutputStream = new FileOutputStream(output)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
    }
}
