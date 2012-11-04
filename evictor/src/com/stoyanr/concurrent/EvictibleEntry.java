package com.stoyanr.concurrent;

import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

class EvictibleEntry<K, V> implements Entry<K, V> {
    private final K key;
    private V value;
    private final long evictMs;
    private final long evictNs;
    private final long createdNs;

    public EvictibleEntry(K key, V value, long evictMs) {
        this(key, value, evictMs, System.nanoTime());
    }

    public EvictibleEntry(K key, V value, long evictMs, long createdNs) {
        assert (key != null);
        assert (value != null);
        this.key = key;
        this.value = value;
        this.evictMs = evictMs;
        this.evictNs = TimeUnit.NANOSECONDS.convert(evictMs, TimeUnit.MILLISECONDS);
        this.createdNs = createdNs;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        assert (value != null);
        synchronized (this.value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

    public long getEvictMs() {
        return evictMs;
    }

    public long getCreatedNs() {
        return createdNs;
    }
    
    public boolean shouldEvict() {
        return (evictNs > 0)? (System.nanoTime() > createdNs + evictNs) : false;
    }
/*
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        EvictibleEntry<K, V> e = (EvictibleEntry<K, V>) o;
        return (this.key.equals(e.key) && this.value.equals(e.value));
    }

    @Override
    public int hashCode() {
        return (this.key.hashCode() ^ this.value.hashCode());
    }
*/
    @Override
    public String toString() {
        return String.format("key: %s, value: %s, evictMs: %d, createdNs: %d", getKey(),
            getValue(), getEvictMs(), getCreatedNs());
    }
}