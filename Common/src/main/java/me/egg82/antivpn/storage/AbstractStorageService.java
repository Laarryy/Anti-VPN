package me.egg82.antivpn.storage;

import io.ebean.Database;
import java.time.Instant;
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

    /*
    Note: Can be an expensive operation
     */
    public IPModel getIpModel(String ip, long cacheTimeMillis) {
        if (ip == null) {
            throw new IllegalArgumentException("ip cannot be null.");
        }

        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .ip.equalTo(ip)
                .findOne();
        queueLock.readLock().unlock();
        return model == null || model.getModified().isBefore(Instant.now().minusMillis(cacheTimeMillis)) ? null : model;
    }

    public IPModel getIpModel(long ipId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        IPModel model = new QIPModel(connection)
                .id.equalTo(ipId)
                .findOne();
        queueLock.readLock().unlock();
        return model == null || model.getModified().isBefore(Instant.now().minusMillis(cacheTimeMillis)) ? null : model;
    }

    /*
    Note: Can be an expensive operation
     */
    public PlayerModel getPlayerModel(UUID player, long cacheTimeMillis) {
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null.");
        }

        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .uuid.equalTo(player)
                .findOne();
        queueLock.readLock().unlock();
        return model == null || model.getModified().isBefore(Instant.now().minusMillis(cacheTimeMillis)) ? null : model;
    }

    public PlayerModel getPlayerModel(long playerId, long cacheTimeMillis) {
        queueLock.readLock().lock();
        PlayerModel model = new QPlayerModel(connection)
                .id.equalTo(playerId)
                .findOne();
        queueLock.readLock().unlock();
        return model == null || model.getModified().isBefore(Instant.now().minusMillis(cacheTimeMillis)) ? null : model;
    }
}
