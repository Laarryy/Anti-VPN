package me.egg82.antivpn.api.model.ip;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum AlgorithmMethod {
    CASCADE("cascade"),
    CONSESNSUS("consensus");

    private final String name;
    AlgorithmMethod(@NotNull String name) {
        this.name = name.toLowerCase();
    }

    public String getName() { return name; }

    public static @Nullable AlgorithmMethod getByName(@NotNull String name) {
        for (AlgorithmMethod value : values()) {
            if (value.name.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
