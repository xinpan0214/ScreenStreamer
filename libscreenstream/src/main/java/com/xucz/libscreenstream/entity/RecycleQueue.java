package com.xucz.libscreenstream.entity;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 描述：
 *
 * @author 创建人 ：xucz
 * @since 创建时间 ：2019-06-27
 */
public abstract class RecycleQueue<T> {
    private LinkedBlockingQueue<T> queue;
    private LinkedBlockingQueue<T> cache;
    private int capacity;

    public RecycleQueue(int capacity) {
        this.capacity = capacity;
    }

    public void ready() {
        if (null == this.queue) {
            this.queue = new LinkedBlockingQueue(this.capacity);
        }

        if (null == this.cache) {
            this.cache = new LinkedBlockingQueue(this.capacity);

            for (int i = 0; i < this.capacity; ++i) {
                this.cache.offer(this.newCacheEntry());
            }
        }

    }

    public void offer(T entry) {
        this.queue.offer(entry);
    }

    public T poll() {
        return this.queue.poll();
    }

    public T take() throws InterruptedException {
        return this.queue.take();
    }

    public T pollCache() {
        return this.cache.poll();
    }

    public T takeCache() throws InterruptedException {
        return this.cache.take();
    }

    public synchronized void recycle(T entry) {
        this.cache.offer(entry);
    }

    public abstract T newCacheEntry();

    public void release() {
        this.queue.clear();
        this.cache.clear();
    }

    public int getCapacity() {
        return this.capacity;
    }

    public int getCacheSize() {
        synchronized (this.cache) {
            return this.cache.size();
        }
    }
}
