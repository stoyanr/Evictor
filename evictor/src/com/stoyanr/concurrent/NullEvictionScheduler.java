package com.stoyanr.concurrent;

public class NullEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    @Override
    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        // Do nothing
    }

    @Override
    public void cancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        // Do nothing
    }

}
