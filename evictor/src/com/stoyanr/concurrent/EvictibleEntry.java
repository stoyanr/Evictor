package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Map.Entry;

class EvictibleEntry<K, V> implements Entry<K, V> {
    private final K key;
    private V value;
    private final boolean evictible;
    private final long evictionTime;
    private volatile Object data;

    public EvictibleEntry(K key, V value, long evictMs) {
        assert (key != null);
        assert (value != null);
        this.key = key;
        this.value = value;
        this.evictible = (evictMs > 0);
        this.evictionTime = (evictible) ? now() + NANOSECONDS.convert(evictMs, MILLISECONDS) : 0;
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

    public boolean isEvictible() {
        return evictible;
    }

    public long getEvictionTime() {
        return evictionTime;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public boolean shouldEvict() {
        return (evictible) ? (now() > evictionTime) : false;
    }

    @Override
    public String toString() {
        return String.format("[key: %s, value: %s, evictionTime: %d]", key, value, evictionTime);
    }

    public static long now() {
        return System.nanoTime();
    }

}