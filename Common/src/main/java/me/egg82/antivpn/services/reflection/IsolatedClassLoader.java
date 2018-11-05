package me.egg82.antivpn.services.reflection;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Taken from LuckPerms @ https://github.com/lucko/LuckPerms/blob/ee13540d7886e3c847d48d3524c692430d6a9404/common/src/main/java/me/lucko/luckperms/common/dependencies/classloader/IsolatedClassLoader.java
 */
public class IsolatedClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader() { this(new URL[0]); }

    public IsolatedClassLoader(URL[] urls) { super(urls, ClassLoader.getSystemClassLoader().getParent()); }
}
