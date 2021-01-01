package me.egg82.antivpn.storage;

import io.ebean.Database;
import java.time.Instant;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.storage.models.query.QIPModel;
import me.egg82.antivpn.storage.models.query.QPlayerModel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStorageService implements StorageService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final String name;
    protected Database connection;

    public @NonNull String getName() { return name; }

    private volatile boolean closed = false;
    private final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    protected AbstractStorageService(@NonNull String name) {
        this.name = name;
    }

    public void close() {
        queueLock.writeLock().lock();
        closed = true;
        connection.shutdown(false, false);
        queueLock.writeLock().unlock();
    }

    public boolean isClosed() { return closed; }

    public void storeModel(@NonNull BaseModel model) {
        queueLock.readLock().lock();
        connection.save(model);
        queueLock.readLock().unlock();
    }

    public void storeModels(@NonNull Collection<? extends BaseModel> models) {
        queueLock.readLock().lock();
        connection.saveAll(models);
        queueLock.readLock().unlock();
    }

    public void deleteModel(@NonNull BaseModel model) {
        queueLock.readLock().lock();
        connection.delete(model);
        queueLock.readLock().unlock();
    }

    public @Nullable IPModel getIpModel(@NonNull String ip, long cacheTimeMillis) {
        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .ip.equalTo(ip)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public @Nullable IPModel getIpModel(long ipId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .id.equalTo(ipId)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public @NonNull Set<IPModel> getAllIps(long cacheTimeMillis) {
        queueLock.readLock().lock();
        Set<IPModel> models = new QIPModel(connection)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }

    public @NonNull Set<IPModel> getAllIps(int start, int end) {
        queueLock.readLock().lock();
        Set<IPModel> models = new QIPModel(connection)
                .id.between(start - 1, end + 1)
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }

    public @Nullable PlayerModel getPlayerModel(@NonNull UUID player, long cacheTimeMillis) {
        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .uuid.equalTo(player)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public @Nullable PlayerModel getPlayerModel(long playerId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .id.equalTo(playerId)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public @NonNull Set<PlayerModel> getAllPlayers(long cacheTimeMillis) {
        queueLock.readLock().lock();
        Set<PlayerModel> models = new QPlayerModel(connection)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }

    public @NonNull Set<PlayerModel> getAllPlayers(int start, int end) {
        queueLock.readLock().lock();
        Set<PlayerModel> models = new QPlayerModel(connection)
                .id.between(start - 1, end + 1)
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }
}
