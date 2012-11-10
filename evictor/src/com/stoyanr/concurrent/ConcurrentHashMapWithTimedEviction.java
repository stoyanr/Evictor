package com.stoyanr.concurrent;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapWithTimedEviction<K, V> extends
    ConcurrentMapWithTimedEvictionDecorator<K, V> implements ConcurrentMapWithTimedEviction<K, V> {

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor,
        int concurrencyLevel, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor,
            concurrencyLevel), scheduler);
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor,
        int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, new SingleRegularTaskEvictionScheduler<K, V>());
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor,
        EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor),
            scheduler);
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, new SingleRegularTaskEvictionScheduler<K, V>());
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity), scheduler);
    }

    public ConcurrentHashMapWithTimedEviction(int initialCapacity) {
        this(initialCapacity, new SingleRegularTaskEvictionScheduler<K, V>());
    }

    public ConcurrentHashMapWithTimedEviction(EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(), scheduler);
    }

    public ConcurrentHashMapWithTimedEviction() {
        this(new SingleRegularTaskEvictionScheduler<K, V>());
    }
}
