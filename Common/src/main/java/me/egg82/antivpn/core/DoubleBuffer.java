package me.egg82.antivpn.core;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DoubleBuffer<T> {
    private volatile Queue<T> currentBuffer = new ConcurrentLinkedQueue<>();
    private volatile Queue<T> backBuffer = new ConcurrentLinkedQueue<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public Queue<T> getReadBuffer() {
        lock.readLock().lock();
        Queue<T> t = backBuffer;
        lock.readLock().unlock();
        return t;
    }

    public Queue<T> getWriteBuffer() {
        lock.readLock().lock();
        Queue<T> t = currentBuffer;
        lock.readLock().unlock();
        return t;
    }

    public void swapBuffers() {
        lock.writeLock().lock();
        Queue<T> t = currentBuffer;
        currentBuffer = backBuffer;
        backBuffer = t;
        lock.writeLock().unlock();
    }
}
