package me.egg82.antivpn.bukkit;

import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.List;
import ninja.egg82.reflect.PackageFilter;
import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitVersionUtil {
    private static final Object versionLock = new Object();
    private static String gameVersion = null;

    private BukkitVersionUtil() { }

    public static boolean isAtLeast(@NonNull String version) {
        int[] v1 = parseVersion(version, '.');
        int[] v2 = parseVersion(getGameVersion(), '.');

        boolean equalOrGreater = true;
        for (int i = 0; i < v1.length; i++) {
            if (i > v2.length) {
                // We're looking for a version deeper than what we have
                // eg. 1.12.2 -> 1.12
                equalOrGreater = false;
                break;
            }

            if (v2[i] < v1[i]) {
                // The version we're at now is less than the one we want
                // eg. 1.11 -> 1.13
                equalOrGreater = false;
                break;
            }
        }

        return equalOrGreater;
    }

    public static @NonNull String getGameVersion() {
        String localVersion = gameVersion;
        if (localVersion == null) {
            synchronized (versionLock) {
                localVersion = gameVersion;
                if (localVersion == null) {
                    localVersion = getVersionFromVersionString(Bukkit.getVersion());
                    if (localVersion == null || !isVersion(localVersion)) {
                        localVersion = getVersionFromBukkitString(Bukkit.getBukkitVersion());
                        if (localVersion == null || !isVersion(localVersion)) {
                            localVersion = getVersionFromServerPackageString(Bukkit.getServer().getClass().getPackage().getName());
                            if (!isVersion(localVersion)) {
                                throw new RuntimeException("Could not get version from Bukkit! (Is the server or another plugin changing it?)");
                            }
                        }
                    }
                    gameVersion = localVersion;
                }
            }
        }

        return localVersion;
    }

    private static @Nullable String getVersionFromVersionString(@NonNull String versionString) {
        int versionIndex = versionString.indexOf("(MC: ");
        if (versionIndex == -1) {
            return null;
        }
        int endIndex = versionString.indexOf(')', versionIndex);
        if (endIndex == -1) {
            return null;
        }

        return versionString.substring(versionIndex + 5, endIndex).trim().replace('_', '.');
    }

    private static @Nullable String getVersionFromBukkitString(@NonNull String versionString) {
        int endIndex = versionString.indexOf("-R");
        if (endIndex == -1) {
            return null;
        }

        return versionString.substring(0, endIndex).trim().replace('_', '.');
    }

    private static @NonNull String getVersionFromServerPackageString(@NonNull String versionString) {
        return versionString.substring(versionString.lastIndexOf('.') + 1).trim().replace('_', '.');
    }

    private static boolean isVersion(@NonNull String versionString) {
        String[] numbers = versionString.split("\\.");
        int[] version = parseVersion(versionString, '.');
        if (version.length != numbers.length) {
            return false;
        }

        for (int i = 0; i < numbers.length; i++) {
            if (tryParseInt(numbers[i]) != version[i]) {
                return false;
            }
        }

        return true;
    }

    public static <T> @Nullable Class<T> getBestMatch(@NonNull Class<T> clazz, @NonNull String version, @NonNull String packageName, boolean recursive) {
        List<Class<T>> enums = PackageFilter.getClasses(clazz, packageName, recursive, false, false);

        // Sort by version, ascending
        enums.sort((v1, v2) -> {
            int[] v1Name = parseVersion(v1.getSimpleName(), '_');
            int[] v2Name = parseVersion(v2.getSimpleName(), '_');

            if (v1Name.length == 0) {
                return -1;
            }
            if (v2Name.length == 0) {
                return 1;
            }

            for (int i = 0; i < Math.min(v1Name.length, v2Name.length); i++) {
                if (v1Name[i] < v2Name[i]) {
                    return -1;
                } else if (v1Name[i] > v2Name[i]) {
                    return 1;
                }
            }

            return 0;
        });

        int[] currentVersion = parseVersion(version, '.');

        Class<T> bestMatch = null;

        // Ascending order means it will naturally try to get the highest possible value (lowest -> highest)
        for (Class<T> c : enums) {
            String name = c.getSimpleName();

            int[] reflectVersion = parseVersion(name, '_');

            // Here's where we cap how high we can get, comparing the reflected version to the Bukkit version
            // True makes the initial assumption that the current reflected version is correct
            boolean equalToOrLessThan = true;
            for (int i = 0; i < reflectVersion.length; i++) {
                if (currentVersion.length > i) {
                    if (reflectVersion[i] > currentVersion[i]) {
                        // We do not, in fact, have the correct version
                        equalToOrLessThan = false;
                        break;
                    } else if (currentVersion[i] > reflectVersion[i]) {
                        // We definitely have the correct version. At least until a better one comes along
                        break;
                    }
                } else {
                    // Nope, this isn't the correct version
                    equalToOrLessThan = false;
                    break;
                }
            }
            if (equalToOrLessThan) {
                // Our initial assumption was correct. Use this version until we can find one that's better
                bestMatch = c;
            }
        }

        return bestMatch;
    }

    private static int[] parseVersion(@NonNull String version, char separator) {
        List<Integer> ints = new ArrayList<>();

        int lastIndex = 0;
        int currentIndex = version.indexOf(separator);

        while (currentIndex > -1) {
            int current = tryParseInt(version.substring(lastIndex, currentIndex));
            if (current > -1) {
                ints.add(current);
            }

            lastIndex = currentIndex + 1;
            currentIndex = version.indexOf(separator, currentIndex + 1);
        }
        int current = tryParseInt(version.substring(lastIndex));
        if (current > -1) {
            ints.add(current);
        }

        return Ints.toArray(ints);
    }

    private static int tryParseInt(@NonNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return -1;
        }
    }
}
