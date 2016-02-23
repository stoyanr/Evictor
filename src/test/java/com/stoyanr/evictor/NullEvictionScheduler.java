package com.stoyanr.evictor;

public class NullEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        // Do nothing
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        // Do nothing
    }

    @Override
    public void shutdown() {
        // Do nothing
    }

}
