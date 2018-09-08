package me.egg82.avpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;

import com.google.common.io.Files;

import ninja.egg82.patterns.ServiceLocator;
import ninja.egg82.plugin.utils.DirectoryUtil;
import ninja.egg82.utils.FileUtil;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

public class ConfigLoader {
    // vars

    // constructor
    public ConfigLoader() {

    }

    // public
    public static Configuration getConfig(String resourcePath, String configFileName) {
        File dataDir = ServiceLocator.getService(Plugin.class).getDataFolder();
        if (dataDir.exists() && !dataDir.isDirectory()) {
            dataDir.delete();
        }
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        File configFile = new File(dataDir, configFileName);
        if (configFile.exists() && configFile.isDirectory()) {
            DirectoryUtil.delete(configFile);
        }
        if (!configFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(ServiceLocator.getService(Plugin.class).getResource(resourcePath));
                BufferedReader in = new BufferedReader(reader);
                FileWriter writer = new FileWriter(configFile);
                BufferedWriter out = new BufferedWriter(writer)) {
                String line = null;
                while ((line = in.readLine()) != null) {
                    writer.write(line + FileUtil.LINE_SEPARATOR);
                }
            } catch (Exception ex) {
                throw new RuntimeException("Error writing config. Aborting plugin load.", ex);
            }
        }

        ConfigurationLoader<ConfigurationNode> loader = YAMLConfigurationLoader.builder().setFlowStyle(FlowStyle.BLOCK).setIndent(2).setFile(configFile).build();
        ConfigurationNode root = null;
        try {
            root = loader
                .load(ConfigurationOptions.defaults().setHeader("Comments are gone because update :(. Click here for new config + comments: https://www.spigotmc.org/resources/anti-vpn.58291/"));
        } catch (Exception ex) {
            throw new RuntimeException("Error loading config. Aborting plugin load.", ex);
        }
        Configuration config = new Configuration(root);
        conformVersion(loader, config, configFileName);
        ServiceLocator.provideService(config);

        return config;
    }

    // private
    private static void conformVersion(ConfigurationLoader<ConfigurationNode> loader, ConfigurationNode config, String configFileName) {
        double oldVersion = config.getNode("version").getDouble(1.0d);

        if (config.getNode("version").getDouble(1.0d) == 1.0d) {
            to20(config);
        }
        if (config.getNode("version").getDouble() == 2.0d) {
            to21(config);
        }

        if (config.getNode("version").getDouble() != oldVersion) {
            File backupFile = new File(ServiceLocator.getService(Plugin.class).getDataFolder(), configFileName + ".bak");
            if (backupFile.isDirectory()) {
                DirectoryUtil.delete(backupFile);
            } else {
                backupFile.delete();
            }

            File configFile = new File(ServiceLocator.getService(Plugin.class).getDataFolder(), configFileName);

            try {
                Files.copy(configFile, backupFile);
            } catch (Exception ex) {
                throw new RuntimeException("Error writing backup file. Aborting plugin load.", ex);
            }
            try {
                loader.save(config);
            } catch (Exception ex) {
                throw new RuntimeException("Error writing config. Aborting plugin load.", ex);
            }
        }
    }

    private static void to20(ConfigurationNode config) {
        // Rabbit -> Messaging
        boolean rabbitEnabled = config.getNode("rabbit", "enabled").getBoolean();
        String rabbitAddress = config.getNode("rabbit", "address").getString("");
        int rabbitPort = config.getNode("rabbit", "port").getInt(5672);
        String rabbitUser = config.getNode("rabbit", "user").getString("guest");
        String rabbitPass = config.getNode("rabbit", "pass").getString("guest");
        config.removeChild("rabbit");
        config.getNode("messaging", "type").setValue((rabbitEnabled) ? "rabbit" : "bungee");
        config.getNode("messaging", "rabbit", "address").setValue(rabbitAddress);
        config.getNode("messaging", "rabbit", "port").setValue(Integer.valueOf(rabbitPort));
        config.getNode("messaging", "rabbit", "user").setValue(rabbitUser);
        config.getNode("messaging", "rabbit", "pass").setValue(rabbitPass);

        // sources.order String -> List
        String[] order = config.getNode("sources", "order").getString().split(",\\s?");
        config.getNode("sources", "order").setValue(Arrays.asList(order));

        // Add ignore
        config.getNode("ignore").setValue(Arrays.asList(new String[] { "127.0.0.1", "localhost", "::1" }));

        // Remove async
        config.removeChild("async");

        // Version
        config.getNode("version").setValue(Double.valueOf(2.0d));
    }
    private static void to21(ConfigurationNode config) {
        // Add consensus
        config.getNode("consensus").setValue(Double.valueOf(-1.0d));
    }
}
