package me.egg82.antivpn.locale;

import java.io.*;
import java.nio.file.Files;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LanguageFileUtil {
    private LanguageFileUtil() { }

    public static @Nullable ResourceBundle getLanguage(@NotNull File dataDirectory, @NotNull Locale locale) throws IOException {
        return getLanguage(
                dataDirectory,
                locale,
                false
        );
    }

    public static @Nullable ResourceBundle getLanguage(@NotNull File dataDirectory, @NotNull Locale locale, boolean ignoreCountry) throws IOException {
        // Build resource path & file path for language
        // Use country is specified (and lang provides country)
        String resourcePath = ignoreCountry || locale.getCountry() == null || locale.getCountry().isEmpty()
                              ? "lang_" + locale.getLanguage() + ".properties"
                              : "lang_" + locale.getLanguage() + "_" + locale.getCountry() + ".properties";
        File langDir = new File(dataDirectory, "lang");
        File fileOnDisk = new File(langDir, resourcePath);

        // Clean up/build language path on disk
        if (langDir.exists() && !langDir.isDirectory()) {
            Files.delete(langDir.toPath());
        }
        if (!langDir.exists()) {
            if (!langDir.mkdirs()) {
                throw new IOException(LocaleUtil.getDefaultI18N().getText(MessageKey.ERROR__PARENT_DIR));
            }
        }
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        ResourceBundle resourceBundle;
        try (InputStream inStream = LanguageFileUtil.class.getResourceAsStream("/lang/" + resourcePath)) {
            if (inStream != null) {
                resourceBundle = new LocalePropertyResourceBundle(inStream, locale);
            } else {
                if (fileOnDisk.exists()) {
                    // Return file on disk
                    try (FileInputStream fileStream = new FileInputStream(fileOnDisk)) {
                        return new LocalePropertyResourceBundle(fileStream, locale);
                    }
                } else {
                    // If we need a more generic language (eg. if we have "en_US" and we don't have "en_US.yml" but we do have "en.yml") then return the more generic language file
                    // Otherwise, no language found
                    return ignoreCountry ? null : getLanguage(dataDirectory, locale, true);
                }
            }
        }

        // Check language version
        if (fileOnDisk.exists()) {
            double resourceVersion = resourceBundle.containsKey("version") ? Double.parseDouble(resourceBundle.getString("version")) : 1.0d;

            ResourceBundle fileBundle;
            try (FileInputStream fileStream = new FileInputStream(fileOnDisk)) {
                fileBundle = new LocalePropertyResourceBundle(fileStream, locale);
            }
            double fileVersion = fileBundle.containsKey("version") ? Double.parseDouble(fileBundle.getString("version")) : 1.0d;

            if (resourceVersion > fileVersion) {
                // Version update, backup & delete file on disk
                File backupFile = new File(fileOnDisk.getParent(), fileOnDisk.getName() + ".bak");
                if (backupFile.exists()) {
                    Files.delete(backupFile.toPath());
                }

                com.google.common.io.Files.copy(fileOnDisk, backupFile);
                Files.delete(fileOnDisk.toPath());
            }
        }

        // Write language file to disk if not exists
        if (!fileOnDisk.exists()) {
            try (InputStream inStream = LanguageFileUtil.class.getResourceAsStream("/lang/" + resourcePath)) {
                if (inStream != null) {
                    try (FileOutputStream outStream = new FileOutputStream(fileOnDisk)) {
                        int read;
                        byte[] buffer = new byte[4096];
                        while ((read = inStream.read(buffer, 0, buffer.length)) > 0) {
                            outStream.write(buffer, 0, read);
                        }
                    }
                }
            }
        }

        if (fileOnDisk.exists()) {
            // Return file on disk
            try (FileInputStream fileStream = new FileInputStream(fileOnDisk)) {
                return new LocalePropertyResourceBundle(fileStream, locale);
            }
        } else {
            // If we need a more generic language (eg. if we have "en_US" and we don't have "en_US.yml" but we do have "en.yml") then return the more generic language file
            // Otherwise, no language found
            return ignoreCountry ? null : getLanguage(dataDirectory, locale, true);
        }
    }
}
