/**
 * 
 * Copyright 2012, Stoyan Rachev
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stoyanr.evictor.queue;

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.stoyanr.evictor.EvictionQueue;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * An {@link EvictionQueue} which uses a
 * {@link java.util.concurrent.ConcurrentNavigableMap} to store its entries. The
 * key in the map is the eviction time of the entry, and the value is the entry
 * itself.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class NavigableMapEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map;

    /**
     * Creates a navigable map eviction queue with a
     * {@link java.util.concurrent.ConcurrentSkipListMap}.
     */
    public NavigableMapEvictionQueue() {
        this(new ConcurrentSkipListMap<Long, EvictibleEntry<K, V>>());
    }

    /**
     * Creates a navigable map eviction queue with the specified map.
     * 
     * @param map
     *            the map to be used
     * 
     * @throws NullPointerException
     *             if the map is <code>null</code>
     */
    public NavigableMapEvictionQueue(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> map) {
        if (map == null) {
            throw new NullPointerException("Map instnace cannot be null");
        }

        this.map = map;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns true if the map is non-empty and vice
     * versa.
     * </p>
     */
    @Override
    public boolean hasEntries() {
        return !map.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns the first key in the map if it is
     * non-empty, or 0 otherwise.
     * </p>
     */
    @Override
    public long getNextEvictionTime() {
        try {
            return (!map.isEmpty()) ? map.firstKey() : 0;
        } catch (NoSuchElementException e) {
            // Safeguard in a concurrent environment
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>put</tt> method on the map.
     */
    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        map.put(e.getEvictionTime(), e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>remove</tt> method on the map.
     * </p>
     */
    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        map.remove(e.getEvictionTime(), e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation uses the <tt>headMap</tt> method on the map to find
     * all entries that should be evicted, and then calls the <tt>evict</tt>
     * method on each one of them and removes them from the map.
     * </p>
     */
    @Override
    public boolean evictEntries() {
        boolean result = false;
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = map.headMap(System.nanoTime());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                e.evict(false);
            }
            head.clear();
            result = true;
        }
        return result;
    }

}
