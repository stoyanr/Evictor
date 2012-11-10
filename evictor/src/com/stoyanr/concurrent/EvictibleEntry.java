package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Map.Entry;

class EvictibleEntry<K, V> implements Entry<K, V> {
    private final K key;
    private V value;
    private final long evictMs;
    private final long expiredNs;
    private volatile Object data;

    public EvictibleEntry(K key, V value, long evictMs) {
        this(key, value, evictMs, System.nanoTime());
    }

    public EvictibleEntry(K key, V value, long evictMs, long createdNs) {
        assert (key != null);
        assert (value != null);
        this.key = key;
        this.value = value;
        this.evictMs = evictMs;
        this.expiredNs = createdNs + NANOSECONDS.convert(evictMs, MILLISECONDS);
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

    public long getExpiredNs() {
        return expiredNs;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean shouldEvict() {
        return (evictMs > 0) ? (System.nanoTime() > expiredNs) : false;
    }

    @Override
    public String toString() {
        return String.format("[key: %s, value: %s, evictMs: %d]", getKey(), getValue(),
            getEvictMs());
    }

}