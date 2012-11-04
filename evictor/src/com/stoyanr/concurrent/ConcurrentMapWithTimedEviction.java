package com.stoyanr.concurrent;

import java.util.concurrent.ConcurrentMap;

public interface ConcurrentMapWithTimedEviction<K, V> extends ConcurrentMap<K, V> {

    public V put(K key, V value, long evictMs);

    public V putIfAbsent(K key, V value, long evictMs);

    public V replace(K key, V value, long evictMs);

    public boolean replace(K key, V oldValue, V newValue, long evictMs);
}
