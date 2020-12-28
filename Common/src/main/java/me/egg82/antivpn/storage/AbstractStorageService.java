package me.egg82.antivpn.storage;

import io.ebean.Database;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import me.egg82.antivpn.storage.models.BaseModel;
import me.egg82.antivpn.storage.models.IPModel;
import me.egg82.antivpn.storage.models.PlayerModel;
import me.egg82.antivpn.storage.models.query.QIPModel;
import me.egg82.antivpn.storage.models.query.QPlayerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStorageService implements StorageService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String name;
    protected Database connection;

    public String getName() { return name; }

    private volatile boolean closed = false;
    private final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    public void close() {
        queueLock.writeLock().lock();
        closed = true;
        connection.shutdown(false, false);
        queueLock.writeLock().unlock();
    }

    public boolean isClosed() { return closed; }

    public void storeModel(BaseModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null.");
        }

        queueLock.readLock().lock();
        connection.save(model);
        queueLock.readLock().unlock();
    }

    public void deleteModel(BaseModel model) {
        if (model == null) {
            throw new IllegalArgumentException("model cannot be null.");
        }

        queueLock.readLock().lock();
        connection.delete(model);
        queueLock.readLock().unlock();
    }

    public IPModel getIpModel(String ip, long cacheTimeMillis) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .ip.equalTo(ip)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public IPModel getIpModel(long ipId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .id.equalTo(ipId)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public Set<IPModel> getAllIps(long cacheTimeMillis) {
        queueLock.readLock().lock();
        Set<IPModel> models = new QIPModel(connection)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }

    public PlayerModel getPlayerModel(UUID player, long cacheTimeMillis) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null.");
        }

        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .uuid.equalTo(player)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public PlayerModel getPlayerModel(long playerId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .id.equalTo(playerId)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findOne();
        queueLock.readLock().unlock();
        return model;
    }

    public Set<PlayerModel> getAllPlayers(long cacheTimeMillis) {
        queueLock.readLock().lock();
        Set<PlayerModel> models = new QPlayerModel(connection)
                .modified.after(Instant.now().minusMillis(cacheTimeMillis))
                .findSet();
        queueLock.readLock().unlock();
        return models;
    }
}
