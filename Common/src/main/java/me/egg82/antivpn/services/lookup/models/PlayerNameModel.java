package me.egg82.antivpn.services.lookup.models;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class PlayerNameModel implements Serializable {
    private String name = null;

    public PlayerNameModel() { }

    public @Nullable String getName() { return name; }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerNameModel)) {
            return false;
        }
        PlayerNameModel that = (PlayerNameModel) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }

    @Override
    public String toString() {
        return "PlayerNameModel{" +
                "name='" + name + '\'' +
                '}';
    }
}
