package com.stoyanr.evictor;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public abstract class AbstractQueueEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue;

    public AbstractQueueEvictionScheduler() {
        this(new ConcurrentSkipListMap<Long, EvictibleEntry<K, V>>());
    }

    public AbstractQueueEvictionScheduler(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue) {
        super();
        assert (queue != null);
        this.queue = queue;
    }

    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        assert (e != null);
        if (e.isEvictible()) {
            queue.put(e.getEvictionTime(), e);
            onScheduleEviction(e);
        }
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        assert (e != null);
        if (e.isEvictible()) {
            queue.remove(e.getEvictionTime(), e);
            onCancelEviction(e);
        }
    }

    protected void evictEntries() {
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = queue.headMap(System.nanoTime());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                e.evict(false);
            }
            head.clear();
            onEvictEntries();
        }
    }

    protected boolean hasScheduledEvictions() {
        return !queue.isEmpty();
    }

    protected long getNextEvictionTime() {
        try {
            return (!queue.isEmpty()) ? queue.firstKey() : 0;
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    protected abstract void onScheduleEviction(EvictibleEntry<K, V> e);

    protected abstract void onCancelEviction(EvictibleEntry<K, V> e);

    protected abstract void onEvictEntries();

    final class EvictionRunnable implements Runnable {
        @Override
        public void run() {
            evictEntries();
        }
    }
}
