package me.egg82.antivpn.storage.models;

import io.ebean.annotation.NotNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "avpn_6_data")
public class DataModel extends BaseModel {
    @NotNull
    private String key;
    private String value;

    public DataModel() {
        super();
        this.key = "";
        this.value = null;
    }

    public DataModel(@NonNull String dbName) {
        super(dbName);
        this.key = "";
        this.value = null;
    }

    public @NonNull String getKey() { return key; }

    public void setKey(@NonNull String key) { this.key = key; }

    public @Nullable String getValue() { return value; }

    public void setValue(String value) { this.value = value; }

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
