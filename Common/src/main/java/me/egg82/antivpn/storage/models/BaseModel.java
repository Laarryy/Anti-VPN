package me.egg82.antivpn.storage.models;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;

@MappedSuperclass
public abstract class BaseModel extends Model implements Serializable {
    @Id
    protected long id;
    @Version
    protected long version;
    @WhenCreated
    protected Instant created;
    @WhenModified
    protected Instant modified;

    public BaseModel() {
        super();
        this.id = -1L;
        this.version = -1L;
        this.created = null;
        this.modified = null;
    }

    public BaseModel(String dbName) {
        super(dbName);
        this.id = -1L;
        this.version = -1L;
        this.created = null;
        this.modified = null;
    }

    public long getId() { return id; }

    public void setId(long id) { this.id = id; }

    public long getVersion() { return version; }

    public void setVersion(long version) { this.version = version; }

    public Instant getCreated() {  return created; }

    public void setCreated(Instant created) { this.created = created; }

    public Instant getModified() { return modified; }

    public void setModified(Instant modified) { this.modified = modified; }

    public String toString() {
        return "BaseModel{" +
                "id=" + id +
                ", version=" + version +
                ", created=" + created +
                ", modified=" + modified +
                '}';
    }
}
