package com.stoyanr.evictor.scheduler;

import com.stoyanr.evictor.EvictionScheduler;
import com.stoyanr.evictor.map.EvictibleEntry;

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
