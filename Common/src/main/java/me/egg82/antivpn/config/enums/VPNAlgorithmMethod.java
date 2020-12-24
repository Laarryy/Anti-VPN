package me.egg82.antivpn.enums;

public enum VPNAlgorithmMethod {
    CASCADE("cascade"),
    CONSESNSUS("consensus");

    private final String name;
    VPNAlgorithmMethod(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public static VPNAlgorithmMethod getByName(String name) {
        for (VPNAlgorithmMethod value : values()) {
            if (value.name.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
