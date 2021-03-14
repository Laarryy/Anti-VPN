package me.egg82.antivpn.services.lookup.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProfileModel implements Serializable {
    private String id;
    private String name;
    private List<ProfilePropertyModel> properties;

    public ProfileModel() {
        this.id = null;
        this.name = null;
        this.properties = new ArrayList<>();
    }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProfilePropertyModel> getProperties() { return properties; }

    public void setProperties(List<ProfilePropertyModel> properties) {
        this.properties = properties;
    }

    @Override
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

    @Override
    public int hashCode() { return Objects.hash(id, name, properties); }

    @Override
    public String toString() {
        return "ProfileModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", properties=" + properties +
                '}';
    }

    public static class ProfilePropertyModel implements Serializable {
        private String name;
        private String value;
        private String signature;

        public ProfilePropertyModel() {
            this.name = null;
            this.value = null;
            this.signature = null;
        }

        public String getName() { return name; }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() { return value; }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean hasSignature() { return signature != null; }

        public String getSignature() { return signature; }

        public void setSignature(String signature) {
            this.signature = signature;
        }

        @Override
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

        @Override
        public int hashCode() { return Objects.hash(name, value, signature); }

        @Override
        public String toString() {
            return "ProfilePropertyModel{" +
                    "name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", signature='" + signature + '\'' +
                    '}';
        }
    }
}
