package com.stoyanr.evictor;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class NavigableMapEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue;

    public NavigableMapEvictionQueue() {
        this(new ConcurrentSkipListMap<Long, EvictibleEntry<K, V>>());
    }

    public NavigableMapEvictionQueue(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue) {
        assert (queue != null);
        this.queue = queue;
    }

    @Override
    public boolean hasEntries() {
        return !queue.isEmpty();
    }

    @Override
    public long getNextEvictionTime() {
        try {
            return (!queue.isEmpty()) ? queue.firstKey() : 0;
        } catch (NoSuchElementException e) {
            return 0;
        }
    }

    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        queue.put(e.getEvictionTime(), e);
    }

    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        queue.remove(e.getEvictionTime(), e);
    }

    @Override
    public boolean evictEntries() {
        boolean result = false;
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = queue.headMap(System.nanoTime());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                e.evict(false);
            }
            head.clear();
            result = true;
        }
        return result;
    }

}
