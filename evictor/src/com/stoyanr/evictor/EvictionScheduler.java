package com.stoyanr.evictor;

/**
 * An eviction scheduler used by {@link com.stoyanr.evictor.ConcurrentMapWithTimedEvictionDecorator}
 * to automatically evict entries when the time they are allowed to stay in the map (
 * <tt>evictMs</tt>) has elapsed. It provides methods for scheduling the eviction of newly added
 * entries as well as canceling the eviction of entries that have been removed form the map.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by the map
 * @param <V> the type of mapped values
 */
public interface EvictionScheduler<K, V> {

    /**
     * Schedules the eviction of the specified entry from the specified map. This method is called
     * by the associated {@link com.stoyanr.evictor.ConcurrentMapWithTimedEvictionDecorator}
     * whenever a new entry is added to the map, just after it has been added. The entry is not
     * guaranteed to be evictible, it may also be a permanent entry. Therefore, the implementation
     * should check if this entry is really evictible before doing any scheduling.
     * 
     * @param map the map for which the eviction should be scheduled
     * @param e the entry for which the eviction should be scheduled, if evictible; it must have
     * been already added to the specified map
     * @throws NullPointerException if either the map or the entry is null
     */
    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e);

    /**
     * Cancels the eviction of the specified entry from the specified map. This method is called by
     * the associated {@link com.stoyanr.evictor.ConcurrentMapWithTimedEvictionDecorator} whenever
     * an entry is removed from the map, just after it has been removed. The entry is not guaranteed
     * to be evictible, it may also be a permanent entry. Therefore, the implementation should check
     * if this entry is really evictible before doing any cancellation.
     * 
     * @param map the map for which the eviction should be cancelled
     * @param e the entry for which the eviction should be cancelled, if evictible; it must have
     * been already removed from the specified map
     * @throws NullPointerException if either the map or the entry is null
     */
    public void cancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e);

    /**
     * Cancels all pending evictions for the specified map. This method is called by the associated
     * {@link com.stoyanr.evictor.ConcurrentMapWithTimedEvictionDecorator} whenever all entries are
     * removed from the map, just after they have been removed.
     * 
     * @param map the map for which all pending evictions should be cancelled
     * @throws NullPointerException if the map is null
     */
    public void cancelAllEvictions(ConcurrentMapWithTimedEvictionDecorator<K, V> map);
}
