package me.egg82.antivpn.services.lookup.models;

import java.io.Serializable;
import java.util.Objects;

public class PlayerUUIDModel implements Serializable {
    private String name;
    private String id;

    public PlayerUUIDModel() {
        this.name = null;
        this.id = null;
    }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() { return id; }

    public void setId(String id) {
        this.id = id;
    }

    @Override
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

    @Override
    public int hashCode() { return Objects.hash(name, id); }

    @Override
    public String toString() {
        return "PlayerModel{" +
                "name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
