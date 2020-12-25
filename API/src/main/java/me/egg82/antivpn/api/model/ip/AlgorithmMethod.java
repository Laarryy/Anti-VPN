package me.egg82.antivpn.api.model.ip;

public enum AlgorithmMethod {
    CASCADE("cascade"),
    CONSESNSUS("consensus");

    private final String name;
    AlgorithmMethod(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public static AlgorithmMethod getByName(String name) {
        for (AlgorithmMethod value : values()) {
            if (value.name.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
