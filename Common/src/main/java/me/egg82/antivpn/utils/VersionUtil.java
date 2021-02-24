package me.egg82.antivpn.utils;

import com.google.common.primitives.Ints;
import java.util.ArrayList;
import java.util.List;
import me.egg82.antivpn.reflect.PackageFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VersionUtil {
    private VersionUtil() { }

    public static boolean isAtLeast(@NotNull String atLeastVersion, char separator1, @NotNull String versionToTest, char separator2) {
        int[] v1 = parseVersion(atLeastVersion, separator1);
        int[] v2 = parseVersion(versionToTest, separator2);

        boolean equalOrGreater = true;
        for (int i = 0; i < v1.length; i++) {
            if (i > v2.length) {
                // We're looking for a version deeper than what we have
                // eg. 1.12.2 -> 1.12
                equalOrGreater = false;
                break;
            }
            if (v2[i] > v1[i]) {
                // The version we're at now is greater than the one we want
                // eg. 1.11 -> 1.13
                break;
            }
            if (v2[i] < v1[i]) {
                // The version we're at now is less than the one we want
                // eg. 1.13 -> 1.11
                equalOrGreater = false;
                break;
            }
        }

        return equalOrGreater;
    }

    public static <T> @Nullable Class<T> getBestMatch(@NotNull Class<T> clazz, @NotNull String version, @NotNull String packageName, boolean recursive) {
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

    public static int @NotNull [] parseVersion(@NotNull String version, char separator) {
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

    public static int tryParseInt(@NotNull String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ex) {
            return -1;
        }
    }
}
