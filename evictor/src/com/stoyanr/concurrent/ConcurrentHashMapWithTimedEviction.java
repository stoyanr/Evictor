package com.stoyanr.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapWithTimedEviction<K, V> extends ConcurrentHashMap<K, V> implements
    ConcurrentMapWithTimedEviction<K, V> {

    private static final long serialVersionUID = -6441887222004599097L;

    /**
     * Creates a new, empty map with the specified initial capacity, load factor and concurrency
     * level.
     * 
     * @param initialCapacity the initial capacity. The implementation performs internal sizing to
     * accommodate this many elements.
     * @param loadFactor the load factor threshold, used to control resizing. Resizing may be
     * performed when the average number of elements per bin exceeds this threshold.
     * @param concurrencyLevel the estimated number of concurrently updating threads. The
     * implementation performs internal sizing to try to accommodate this many threads.
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor or
     * concurrencyLevel are nonpositive.
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor,
        int concurrencyLevel) {
        super(initialCapacity, loadFactor, concurrencyLevel);
    }

    /**
     * Creates a new, empty map with the specified initial capacity and load factor and with the
     * default concurrencyLevel (16).
     * 
     * @param initialCapacity The implementation performs internal sizing to accommodate this many
     * elements.
     * @param loadFactor the load factor threshold, used to control resizing. Resizing may be
     * performed when the average number of elements per bin exceeds this threshold.
     * @throws IllegalArgumentException if the initial capacity of elements is negative or the load
     * factor is nonpositive
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Creates a new, empty map with the specified initial capacity, and with default load factor
     * (0.75) and concurrencyLevel (16).
     * 
     * @param initialCapacity the initial capacity. The implementation performs internal sizing to
     * accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of elements is negative.
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Creates a new, empty map with a default initial capacity (16), load factor (0.75) and
     * concurrencyLevel (16).
     */
    public ConcurrentHashMapWithTimedEviction() {
        super();
    }

    /**
     * Creates a new map with the same mappings as the given map. The map is created with a capacity
     * of 1.5 times the number of mappings in the given map or 16 (whichever is greater), and a
     * default load factor (0.75) and concurrencyLevel (16).
     * 
     * @param m the map
     */
    public ConcurrentHashMapWithTimedEviction(Map<? extends K, ? extends V> m) {
        super(m);
    }

    @Override
    public V put(K key, V value, long evictMs) {
        return put(key, value);
    }

    @Override
    public V putIfAbsent(K key, V value, long evictMs) {
        return putIfAbsent(key, value);
    }

    @Override
    public V replace(K key, V value, long evictMs) {
        return replace(key, value);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long evictMs) {
        return replace(key, oldValue, newValue);
    }

}
