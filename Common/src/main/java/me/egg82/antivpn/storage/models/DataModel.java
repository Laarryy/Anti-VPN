package me.egg82.antivpn.storage.models;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Entity
@Table(name = "avpn_6_data")
public class DataModel extends BaseModel {
    @io.ebean.annotation.NotNull
    private String key;
    private String value;

    public DataModel() {
        super();
        this.key = "";
        this.value = null;
    }

    public DataModel(@NotNull String dbName) {
        super(dbName);
        this.key = "";
        this.value = null;
    }

    public @NotNull String getKey() {
        return key;
    }

    public void setKey(@NotNull String key) {
        this.key = key;
    }

    public @Nullable String getValue() {
        return value;
    }

    public void setValue(@Nullable String value) {
        this.value = value;
    }

    public String toString() {
        return "DataModel{" +
                "id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
