package com.stoyanr.concurrent;

public interface EvictionScheduler<K, V> {

    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e);

    public void cancelEviction(EvictibleEntry<K, V> e);

}
