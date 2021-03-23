package me.egg82.antivpn.core;

import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DoubleBuffer<T> {
    private volatile @NotNull Queue<T> currentBuffer = new ConcurrentLinkedQueue<>();
    private volatile @NotNull Queue<T> backBuffer = new ConcurrentLinkedQueue<>();
    private final @NotNull ReadWriteLock lock = new ReentrantReadWriteLock();

    @NotNull
    public Queue<T> getReadBuffer() {
        lock.readLock().lock();
        try {
            return backBuffer;
        } finally {
            lock.readLock().unlock();
        }
    }

    @NotNull
    public Queue<T> getWriteBuffer() {
        lock.readLock().lock();
        try {
            return currentBuffer;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void swapBuffers() {
        lock.writeLock().lock();
        try {
            Queue<T> t = currentBuffer;
            currentBuffer = backBuffer;
            backBuffer = t;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
