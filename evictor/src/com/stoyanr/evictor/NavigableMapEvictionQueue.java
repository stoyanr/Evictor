package com.stoyanr.evictor;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * An {@link EvictionQueue} which uses a {@link java.util.concurrent.ConcurrentNavigableMap} to
 * store its entries. The key in the map is the eviction time of the entry, and the value is the
 * entry itself.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class NavigableMapEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map;

    /**
     * Creates a navigable map eviction queue with a
     * {@link java.util.concurrent.ConcurrentSkipListMap}.
     */
    public NavigableMapEvictionQueue() {
        this(new ConcurrentSkipListMap<Long, EvictibleEntry<K, V>>());
    }

    /**
     * Creates a navigable map eviction queue with the specified map.
     * 
     * @param map the map to be used
     * @throws NullPointerException if the map is null
     */
    public NavigableMapEvictionQueue(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map) {
        if (map == null)
            throw new NullPointerException();
        this.map = map;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns true if the map is non-empty and vice versa.
     */
    @Override
    public boolean hasEntries() {
        return !map.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns the first key in the map if it is non-empty, or 0
     * otherwise.
     */
    @Override
    public long getNextEvictionTime() {
        try {
            return (!map.isEmpty()) ? map.firstKey() : 0;
        } catch (NoSuchElementException e) {
            // Safeguard in a concurrent environment
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>put</tt> method on the map.
     */
    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        map.put(e.getEvictionTime(), e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>remove</tt> method on the map.
     */
    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        map.remove(e.getEvictionTime(), e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation uses the <tt>headMap</tt> method on the map to find all entries that
     * should be evicted, and then calls the <tt>evict</tt> method on each one of them and removes
     * them from the map.
     */
    @Override
    public boolean evictEntries() {
        boolean result = false;
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = map.headMap(System.nanoTime());
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
