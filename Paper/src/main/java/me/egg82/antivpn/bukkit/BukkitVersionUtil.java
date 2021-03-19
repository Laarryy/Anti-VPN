package me.egg82.antivpn.bukkit;

import me.egg82.antivpn.utils.VersionUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitVersionUtil {
    private static final Object versionLock = new Object();
    private static String gameVersion = null;

    private BukkitVersionUtil() { }

    public static @NotNull String getGameVersion() {
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
                                if (localVersion.equalsIgnoreCase("mockbukkit")) {
                                    return "1.8";
                                } else {
                                    throw new RuntimeException("Could not get version from Bukkit! (Is the server or another plugin changing it?)");
                                }
                            }
                        }
                    }
                    gameVersion = localVersion;
                }
            }
        }

        return localVersion;
    }

    private static boolean isVersion(@NotNull String versionString) {
        String[] numbers = versionString.split("\\.");
        int[] version = VersionUtil.parseVersion(versionString, '.');
        if (version.length != numbers.length) {
            return false;
        }

        for (int i = 0; i < numbers.length; i++) {
            if (VersionUtil.tryParseInt(numbers[i]) != version[i]) {
                return false;
            }
        }

        return true;
    }

    private static @Nullable String getVersionFromVersionString(@NotNull String versionString) {
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

    private static @Nullable String getVersionFromBukkitString(@NotNull String versionString) {
        int endIndex = versionString.indexOf("-R");
        if (endIndex == -1) {
            return null;
        }

        return versionString.substring(0, endIndex).trim().replace('_', '.');
    }

    private static @NotNull String getVersionFromServerPackageString(@NotNull String versionString) {
        return versionString.substring(versionString.lastIndexOf('.') + 1)
                .trim()
                .replace('_', '.');
    }
}
