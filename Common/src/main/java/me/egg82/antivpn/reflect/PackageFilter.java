package me.egg82.antivpn.reflect;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import me.egg82.antivpn.config.ConfigUtil;
import me.egg82.antivpn.logging.GELFLogger;
import org.jetbrains.annotations.NotNull;
import org.reflections8.ReflectionUtils;
import org.reflections8.Reflections;
import org.reflections8.scanners.ResourcesScanner;
import org.reflections8.scanners.SubTypesScanner;
import org.reflections8.scanners.TypeElementsScanner;
import org.reflections8.util.ClasspathHelper;
import org.reflections8.util.ConfigurationBuilder;
import org.reflections8.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackageFilter {
    private static final Logger logger = new GELFLogger(LoggerFactory.getLogger(PackageFilter.class));

    private PackageFilter() { }

    public static <T> @NotNull List<@NotNull Class<T>> getClasses(@NotNull Class<T> clazz, @NotNull String pkg, boolean recursive, boolean keepInterfaces, boolean keepAbstracts, String... excludePackages) {
        String excludeString = null;
        if (excludePackages != null && excludePackages.length > 0) {
            for (int i = 0; i < excludePackages.length; i++) {
                excludePackages[i] = "-" + excludePackages[i];
            }
            excludeString = String.join(", ", excludePackages);
        }

        ConfigurationBuilder config = new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false),
                        new ResourcesScanner(),
                        new TypeElementsScanner())
                .setUrls(ClasspathHelper.forPackage(pkg, PackageFilter.class.getClassLoader()));

        if (excludeString != null) {
            config = config.filterInputsBy(FilterBuilder.parsePackages(excludeString).include(FilterBuilder.prefix(pkg)));
        } else {
            config = config.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(pkg)));
        }

        Reflections ref = new Reflections(config);

        Set<String> typeSet = ref.getStore().get("TypeElementsScanner").keySet();
        Set<Class<?>> set = new HashSet<>(ReflectionUtils.forNames(typeSet, ref.getConfiguration().getClassLoaders()));
        ArrayList<Class<T>> list = new ArrayList<>();

        for (Class<?> next : set) {
            if (!keepInterfaces && next.isInterface()) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding interface " + next.getName());
                }
                continue;
            }
            if (!keepAbstracts && Modifier.isAbstract(next.getModifiers())) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding abstract " + next.getName());
                }
                continue;
            }

            String n = next.getName();
            n = n.substring(n.indexOf('.') + 1);

            if (n.contains("$")) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding partial " + next.getName());
                }
                continue;
            }

            if (!recursive) {
                String p = next.getName();
                p = p.substring(0, p.lastIndexOf('.'));

                if (!p.equalsIgnoreCase(pkg)) {
                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info("Excluding sub-package class " + next.getName());
                    }
                    continue;
                }
            }

            if (!clazz.equals(next) && !clazz.isAssignableFrom(next)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding non-assignable " + next.getName());
                }
                continue;
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Adding " + next.getName());
            }
            list.add((Class<T>) next);
        }

        return list;
    }

    public static <T> @NotNull List<@NotNull Class<? extends T>> getClassesParameterized(@NotNull Class<T> clazz, @NotNull String pkg, boolean recursive, boolean keepInterfaces, boolean keepAbstracts, String... excludePackages) {
        String excludeString = null;
        if (excludePackages != null && excludePackages.length > 0) {
            for (int i = 0; i < excludePackages.length; i++) {
                excludePackages[i] = "-" + excludePackages[i];
            }
            excludeString = String.join(", ", excludePackages);
        }

        ConfigurationBuilder config = new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false),
                        new ResourcesScanner(),
                        new TypeElementsScanner())
                .setUrls(ClasspathHelper.forPackage(pkg, PackageFilter.class.getClassLoader()));

        if (excludeString != null) {
            config = config.filterInputsBy(FilterBuilder.parsePackages(excludeString).include(FilterBuilder.prefix(pkg)));
        } else {
            config = config.filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix(pkg)));
        }

        Reflections ref = new Reflections(config);

        Set<String> typeSet = ref.getStore().get("TypeElementsScanner").keySet();
        Set<Class<?>> set = new HashSet<>(ReflectionUtils.forNames(typeSet, ref.getConfiguration().getClassLoaders()));
        ArrayList<Class<? extends T>> list = new ArrayList<>();

        for (Class<?> next : set) {
            if (!keepInterfaces && next.isInterface()) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding interface " + next.getName());
                }
                continue;
            }
            if (!keepAbstracts && Modifier.isAbstract(next.getModifiers())) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding abstract " + next.getName());
                }
                continue;
            }

            String n = next.getName();
            n = n.substring(n.indexOf('.') + 1);

            if (n.contains("$")) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding partial " + next.getName());
                }
                continue;
            }

            if (!recursive) {
                String p = next.getName();
                p = p.substring(0, p.lastIndexOf('.'));

                if (!p.equalsIgnoreCase(pkg)) {
                    if (ConfigUtil.getDebugOrFalse()) {
                        logger.info("Excluding sub-package class " + next.getName());
                    }
                    continue;
                }
            }

            if (!clazz.equals(next) && !clazz.isAssignableFrom(next)) {
                if (ConfigUtil.getDebugOrFalse()) {
                    logger.info("Excluding non-assignable " + next.getName());
                }
                continue;
            }

            if (ConfigUtil.getDebugOrFalse()) {
                logger.info("Adding " + next.getName());
            }
            list.add((Class<? extends T>) next);
        }

        return list;
    }
}
