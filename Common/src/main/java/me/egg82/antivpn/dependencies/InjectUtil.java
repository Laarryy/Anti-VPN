package me.egg82.antivpn.dependencies;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;

public class InjectUtil {
    public static void injectFile(File file, URLClassLoader loader) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("file cannot be null.");
        }
        if (loader == null) {
            throw new IllegalArgumentException("classLoader cannot be null.");
        }

        if (file.exists() && file.isDirectory()) {
            throw new IOException("file is not a file.");
        }
        if (!file.exists()) {
            throw new IOException("file does not exist.");
        }

        URLClassLoaderAccess.create(loader).addURL(file.toURI().toURL());
    }
}