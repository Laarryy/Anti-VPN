package me.egg82.antivpn.lang;

import java.io.*;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class LanguageFileUtil {
    private LanguageFileUtil() { }

    public static @NonNull Optional<File> getLanguage(@NonNull File dataDirectory, @NonNull Locale locale) throws IOException {
        return getLanguage(dataDirectory, locale, false);
    }

    public static @NonNull Optional<File> getLanguage(@NonNull File dataDirectory, @NonNull Locale locale, boolean ignoreCountry) throws IOException {
        // Build resource path & file path for language
        // Use country is specified (and lang provides country)
        String resourcePath = ignoreCountry || locale.getCountry() == null || locale.getCountry().isEmpty() ? "lang_" + locale.getLanguage() + ".yml" : "lang_" + locale.getLanguage() + "_" + locale.getCountry() + ".yml";
        File langDir = new File(dataDirectory, "lang");
        File fileOnDisk = new File(langDir, resourcePath);

        // Clean up/build language path on disk
        if (langDir.exists() && !langDir.isDirectory()) {
            Files.delete(langDir.toPath());
        }
        if (!langDir.exists()) {
            if (!langDir.mkdirs()) {
                throw new IOException("Could not create parent directory structure.");
            }
        }
        if (fileOnDisk.exists() && fileOnDisk.isDirectory()) {
            Files.delete(fileOnDisk.toPath());
        }

        // Check language version
        if (fileOnDisk.exists()) {
            try (InputStream inStream = LanguageFileUtil.class.getResourceAsStream("/lang/" + resourcePath)) {
                if (inStream != null) {
                    ConfigurationLoader<CommentedConfigurationNode> fileLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).file(fileOnDisk).build();
                    CommentedConfigurationNode fileRoot = fileLoader.load();
                    double fileVersion = fileRoot.node("acf-minecraft", "version").getDouble(1.0d);

                    try (InputStreamReader reader = new InputStreamReader(inStream); BufferedReader in = new BufferedReader(reader)) {
                        ConfigurationLoader<CommentedConfigurationNode> streamLoader = YamlConfigurationLoader.builder().nodeStyle(NodeStyle.BLOCK).indent(2).source(() -> in).build();
                        CommentedConfigurationNode streamRoot = streamLoader.load();
                        double streamVersion = streamRoot.node("acf-minecraft", "version").getDouble(1.0d);

                        if (streamVersion > fileVersion) {
                            // Version update, backup & delete file on disk
                            File backupFile = new File(fileOnDisk.getParent(), fileOnDisk.getName() + ".bak");
                            if (backupFile.exists()) {
                                Files.delete(backupFile.toPath());
                            }

                            com.google.common.io.Files.copy(fileOnDisk, backupFile);
                            Files.delete(fileOnDisk.toPath());
                        }
                    }
                }
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
            return Optional.of(fileOnDisk);
        } else {
            // If we need a more generic language (eg. if we have "en_US" and we don't have "en_US.yml" but we do have "en.yml") then return the more generic language file
            // Otherwise, no language found
            return ignoreCountry ? Optional.empty() : getLanguage(dataDirectory, locale, true);
        }
    }
}
