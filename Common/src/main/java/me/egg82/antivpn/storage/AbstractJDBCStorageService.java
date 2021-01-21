package me.egg82.antivpn.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.Transaction;
import io.ebean.config.DatabaseConfig;
import io.ebean.config.dbplatform.DatabasePlatform;
import java.time.Instant;
import java.util.*;
import javax.persistence.PersistenceException;
import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.DataModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.storage.models.query.QIPModel;
import me.egg82.antivpn.storage.models.query.QPlayerModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractJDBCStorageService extends AbstractStorageService {
    protected Database connection;
    protected HikariDataSource source;

    protected AbstractJDBCStorageService(@NonNull String name) {
        super(name);
    }

    public void close() {
        queueLock.writeLock().lock();
        try {
            closed = true;
            connection.shutdown(false, false);
            source.close();
        } finally {
            queueLock.writeLock().unlock();
        }
    }

    public void storeModel(@NonNull BaseModel model) {
        queueLock.readLock().lock();
        try {
            createOrUpdate(model, false);
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public void storeModels(@NonNull Collection<? extends BaseModel> models) {
        queueLock.readLock().lock();
        try (Transaction tx = connection.beginTransaction()) {
            for (BaseModel model : models) {
                createOrUpdate(model, true);
            }
            tx.commit();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public void deleteModel(@NonNull BaseModel model) {
        BaseModel newModel = duplicateModel(model, true);
        if (newModel == null) {
            return;
        }

        queueLock.readLock().lock();
        try {
            connection.delete(newModel);
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull IPModel getOrCreateIpModel(@NonNull String ip, int type) {
        queueLock.readLock().lock();
        try {
            IPModel model = new QIPModel(connection)
                    .ip.equalTo(ip)
                    .findOne();
            if (model == null) {
                model = new IPModel();
                model.setIp(ip);
                model.setType(type);
                connection.save(model);
                model = new QIPModel(connection)
                        .ip.equalTo(ip)
                        .findOne();
                if (model == null) {
                    throw new PersistenceException("findOne() returned null after saving.");
                }
            }
            if (model.getType() != type) {
                model.setType(type);
                model.setModified(null);
                connection.save(model);
            }
            return model;
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable IPModel getIpModel(@NonNull String ip, long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QIPModel(connection)
                    .ip.equalTo(ip)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable IPModel getIpModel(long ipId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QIPModel(connection)
                    .id.equalTo(ipId)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull Set<IPModel> getAllIps(long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QIPModel(connection)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findSet();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull Set<IPModel> getAllIps(int start, int end) {
        queueLock.readLock().lock();
        try {
            return new QIPModel(connection)
                    .id.between(start - 1, end + 1)
                    .findSet();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull PlayerModel getOrCreatePlayerModel(@NonNull UUID player, boolean isMcLeaks) {
        queueLock.readLock().lock();
        try {
            PlayerModel model = new QPlayerModel(connection)
                    .uuid.equalTo(player)
                    .findOne();
            if (model == null) {
                model = new PlayerModel();
                model.setUuid(player);
                model.setMcleaks(isMcLeaks);
                connection.save(model);
                model = new QPlayerModel(connection)
                        .uuid.equalTo(player)
                        .findOne();
                if (model == null) {
                    throw new PersistenceException("findOne() returned null after saving.");
                }
            }
            if (model.isMcleaks() != isMcLeaks) {
                model.setMcleaks(isMcLeaks);
                model.setModified(null);
                connection.save(model);
            }
            return model;
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable PlayerModel getPlayerModel(@NonNull UUID player, long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QPlayerModel(connection)
                    .uuid.equalTo(player)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable PlayerModel getPlayerModel(long playerId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QPlayerModel(connection)
                    .id.equalTo(playerId)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull Set<PlayerModel> getAllPlayers(long cacheTimeMillis) {
        queueLock.readLock().lock();
        try {
            return new QPlayerModel(connection)
                    .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                    .findSet();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull Set<PlayerModel> getAllPlayers(int start, int end) {
        queueLock.readLock().lock();
        try {
            return new QPlayerModel(connection)
                    .id.between(start - 1, end + 1)
                    .findSet();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @NonNull DataModel getOrCreateDataModel(@NonNull String key, String value) {
        queueLock.readLock().lock();
        try {
            DataModel model = new QDataModel(connection)
                .key.equalTo(key)
                .findOne();
            if (model == null) {
                model = new DataModel();
                model.setKey(key);
                model.setValue(value);
                connection.save(model);
                model = new QDataModel(connection)
                    .key.equalTo(key)
                    .findOne();
                if (model == null) {
                    throw new PersistenceException("findOne() returned null after saving.");
                }
            }
            if (!Objects.equals(model.getValue(), value)) {
                model.setValue(value);
                model.setModified(null);
                connection.save(model);
            }
            return model;
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable DataModel getDataModel(@NonNull String key) {
        queueLock.readLock().lock();
        try {
            return new QDataModel(connection)
                .key.equalTo(key)
                .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    public @Nullable DataModel getDataModel(long dataId) {
        queueLock.readLock().lock();
        try {
            return new QDataModel(connection)
                .id.equalTo(dataId)
                .findOne();
        } finally {
            queueLock.readLock().unlock();
        }
    }

    protected final void createSource(HikariConfig config, DatabasePlatform platform, String scriptPath) {
        config.setAutoCommit(false);
        source = new HikariDataSource(config);
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setDataSource(source);
        dbConfig.setDatabasePlatform(platform);
        dbConfig.setAllQuotedIdentifiers(true);
        dbConfig.setDefaultServer(false);
        dbConfig.setRegister(false);
        dbConfig.setName(name);
        dbConfig.setClasses(Arrays.asList(BaseModel.class, IPModel.class, PlayerModel.class));
        connection = DatabaseFactory.createWithContextClassLoader(dbConfig, getClass().getClassLoader());
        connection.script().run(scriptPath);
    }

    private @Nullable BaseModel duplicateModel(@NonNull BaseModel model, boolean keepModified) {
        BaseModel retVal = null;
        if (model instanceof IPModel) {
            IPModel m = new IPModel();
            m.setIp(((IPModel) model).getIp());
            m.setType(((IPModel) model).getType());
            m.setCascade(((IPModel) model).getCascade());
            m.setConsensus(((IPModel) model).getConsensus());
            retVal = m;
        } else if (model instanceof PlayerModel) {
            PlayerModel m = new PlayerModel();
            m.setUuid(((PlayerModel) model).getUuid());
            m.setMcleaks(((PlayerModel) model).isMcleaks());
            retVal = m;
        } else if (model instanceof DataModel) {
            DataModel m = new DataModel();
            m.setKey(((DataModel) model).getKey());
            m.setValue(((DataModel) model).getValue());
            retVal = m;
        }

        if (retVal != null) {
            retVal.setCreated(model.getCreated());
            retVal.setModified(keepModified ? model.getModified() : null);
        } else {
            logger.error("duplicateModel is returning null.");
        }

        return retVal;
    }

    private void createOrUpdate(@NonNull BaseModel model, boolean keepModified) {
        if (model instanceof IPModel) {
            IPModel m = new QIPModel(connection)
                    .ip.equalTo(((IPModel) model).getIp())
                    .findOne();
            if (m == null) {
                m = (IPModel) duplicateModel(model, keepModified);
                if (m == null) {
                    return;
                }
                connection.save(m);
            } else {
                m.setType(((IPModel) model).getType());
                m.setCascade(((IPModel) model).getCascade());
                m.setConsensus(((IPModel) model).getConsensus());
                m.setCreated(model.getCreated());
                m.setModified(keepModified ? model.getModified() : null);
                connection.update(m);
            }
        } else if (model instanceof PlayerModel) {
            PlayerModel m = new QPlayerModel(connection)
                    .uuid.equalTo(((PlayerModel) model).getUuid())
                    .findOne();
            if (m == null) {
                m = (PlayerModel) duplicateModel(model, keepModified);
                if (m == null) {
                    return;
                }
                connection.save(m);
            } else {
                m.setMcleaks(((PlayerModel) model).isMcleaks());
                m.setCreated(model.getCreated());
                m.setModified(keepModified ? model.getModified() : null);
                connection.update(m);
            }
        } else if (model instanceof DataModel) {
            DataModel m = new QDataModel(connection)
                .key.equalTo(((DataModel) model).getKey())
                .findOne();
            if (m == null) {
                m = (DataModel) duplicateModel(model, keepModified);
                if (m == null) {
                    return;
                }
                connection.save(m);
            } else {
                m.setKey(((DataModel) model).getKey());
                m.setValue(((DataModel) model).getValue());
                m.setCreated(model.getCreated());
                m.setModified(keepModified ? model.getModified() : null);
                connection.update(m);
            }
        }
    }
}
