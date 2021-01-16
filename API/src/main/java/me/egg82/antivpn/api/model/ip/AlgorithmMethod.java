package me.egg82.antivpn.api.model.ip;

import org.checkerframework.checker.nullness.qual.NonNull;

public enum AlgorithmMethod {
    CASCADE("cascade"),
    CONSESNSUS("consensus");

    private final String name;
    AlgorithmMethod(@NonNull String name) {
        this.name = name.toLowerCase();
    }

    public String getName() { return name; }

    public static AlgorithmMethod getByName(@NonNull String name) {
        for (AlgorithmMethod value : values()) {
            if (value.name.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
