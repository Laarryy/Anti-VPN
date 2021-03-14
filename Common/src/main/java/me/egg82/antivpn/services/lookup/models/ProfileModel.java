package me.egg82.antivpn.services.lookup.models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfileModel implements Serializable {
    private String id = null;
    private String name = null;
    private List<ProfilePropertyModel> properties = new ArrayList<>();

    public ProfileModel() { }

    public @Nullable String getId() { return id; }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public @Nullable String getName() { return name; }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @NotNull List<@NotNull ProfilePropertyModel> getProperties() { return properties; }

    public void setProperties(@NotNull List<@NotNull ProfilePropertyModel> properties) {
        this.properties = properties;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProfileModel)) {
            return false;
        }
        ProfileModel that = (ProfileModel) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(properties, that.properties);
    }

    public int hashCode() { return Objects.hash(id, name, properties); }

    public String toString() {
        return "ProfileModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", properties=" + properties +
                '}';
    }

    public static class ProfilePropertyModel implements Serializable {
        private String name = null;
        private String value = null;
        private String signature = null;

        public ProfilePropertyModel() { }

        public @Nullable String getName() { return name; }

        public void setName(@Nullable String name) {
            this.name = name;
        }

        public @Nullable String getValue() { return value; }

        public void setValue(@Nullable String value) {
            this.value = value;
        }

        public boolean hasSignature() { return signature != null; }

        public @Nullable String getSignature() { return signature; }

        public void setSignature(@Nullable String signature) {
            this.signature = signature;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ProfilePropertyModel)) {
                return false;
            }
            ProfilePropertyModel that = (ProfilePropertyModel) o;
            return Objects.equals(name, that.name) &&
                    Objects.equals(value, that.value) &&
                    Objects.equals(signature, that.signature);
        }

        public int hashCode() { return Objects.hash(name, value, signature); }

        public String toString() {
            return "ProfilePropertyModel{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", signature='" + signature + '\'' +
                    '}';
        }
    }
}
