package com.stoyanr.evictor;

public interface EvictionQueue<K, V> {

    public boolean hasEntries();

    public long getNextEvictionTime();

    public void putEntry(EvictibleEntry<K, V> e);

    public void removeEntry(EvictibleEntry<K, V> e);

    public boolean evictEntries();
}
