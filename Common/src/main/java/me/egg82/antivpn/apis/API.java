package me.egg82.antivpn.apis;

import java.util.Optional;
import ninja.leaping.configurate.ConfigurationNode;

public interface API {
    String getName();

    Optional<Boolean> getResult(String ip, ConfigurationNode sourceConfigNode);
}
