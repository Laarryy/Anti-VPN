package me.egg82.antivpn.services.lookup.models;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public class PlayerUUIDModel implements Serializable {
    private String name = null;
    private String id = null;

    public PlayerUUIDModel() { }

    public @Nullable String getName() { return name; }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable String getId() { return id; }

    public void setId(@Nullable String id) {
        this.id = id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlayerUUIDModel)) {
            return false;
        }
        PlayerUUIDModel that = (PlayerUUIDModel) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(id, that.id);
    }

    public int hashCode() { return Objects.hash(name, id); }

    public String toString() {
        return "PlayerModel{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
