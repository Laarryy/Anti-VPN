package me.egg82.antivpn.storage;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStorageService implements StorageService {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected AbstractStorageService(@NonNull String name) {
        this.name = name;
    }

    protected final String name;

    public @NonNull String getName() { return name; }

    protected volatile boolean closed = false;
    protected final ReadWriteLock queueLock = new ReentrantReadWriteLock();

    public boolean isClosed() { return closed; }
}
