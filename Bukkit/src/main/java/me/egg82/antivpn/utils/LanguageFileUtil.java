package me.egg82.antivpn.utils;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;

import java.io.*;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Optional;

public class LanguageFileUtil {
    private LanguageFileUtil() {}

    public static Optional<File> getLanguage(Plugin plugin, Locale locale) throws IOException {
        return getLanguage(plugin, locale, false);
    }

    public static Optional<File> getLanguage(Plugin plugin, Locale locale, boolean ignoreCountry) throws IOException {
        // Build resource path & file path for language
        // Use country is specified (and lang provides country)
        String resourcePath = ignoreCountry || locale.getCountry() == null || locale.getCountry().isEmpty() ? "lang_" + locale.getLanguage() + ".yml" : "lang_" + locale.getLanguage() + "_" + locale.getCountry() + ".yml";
        File langDir = new File(plugin.getDataFolder(), "lang");
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
            try (InputStream inStream = plugin.getResource(resourcePath)) {
                if (inStream != null) {
                    ConfigurationLoader<ConfigurationNode> fileLoader = YAMLConfigurationLoader.builder().setFlowStyle(DumperOptions.FlowStyle.BLOCK).setIndent(2).setFile(fileOnDisk).build();
                    ConfigurationNode fileRoot = fileLoader.load();
                    double fileVersion = fileRoot.getNode("acf-minecraft", "version").getDouble(1.0d);

                    try (InputStreamReader reader = new InputStreamReader(inStream);
                         BufferedReader in = new BufferedReader(reader)) {
                        ConfigurationLoader<ConfigurationNode> streamLoader = YAMLConfigurationLoader.builder().setFlowStyle(DumperOptions.FlowStyle.BLOCK).setIndent(2).setSource(() -> in).build();
                        ConfigurationNode streamRoot = streamLoader.load();
                        double streamVersion = streamRoot.getNode("acf-minecraft", "version").getDouble(1.0d);

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
            try (InputStream inStream = plugin.getResource(resourcePath)) {
                if (inStream != null) {
                    try (InputStreamReader reader = new InputStreamReader(inStream);
                         BufferedReader in = new BufferedReader(reader);
                         FileWriter writer = new FileWriter(fileOnDisk);
                         BufferedWriter out = new BufferedWriter(writer)) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            out.write(line + System.lineSeparator());
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
            return ignoreCountry ? Optional.empty() : getLanguage(plugin, locale, true);
        }
    }
}
