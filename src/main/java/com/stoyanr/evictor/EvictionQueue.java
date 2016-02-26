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

package com.stoyanr.evictor;

import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.scheduler.AbstractQueueEvictionScheduler;

/**
 * A priority queue of evictible entries used by
 * {@link AbstractQueueEvictionScheduler} to store entries that should be
 * evicted automatically in the order in which they should be evicted. It
 * provides methods for adding and removing entries, as well as evicting expired
 * entries.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by the map
 * 
 * @param <V>
 *            the type of mapped values
 */
public interface EvictionQueue<K, V> {

    /**
     * Returns <tt>true</tt> if this queue contains any entries.
     * 
     * @return <tt>true</tt> if this queue contains any entries
     */
    public boolean hasEntries();

    /**
     * Returns the next eviction time of all entries contained in the queue, or
     * 0 if the queue is empty. Since the queue is sorted by the entries
     * eviction time, this is the eviction time of the first entry (head).
     * 
     * @return the next eviction time of all entries contained in the queue, or
     *         0 if the queue is empty
     */
    public long getNextEvictionTime();

    /**
     * Puts the specified evictible entry into the queue.
     * 
     * @param e
     *            the entry to be put into the queue.
     * 
     * @throws NullPointerException
     *             if the entry is <code>null</code>
     */
    public void putEntry(EvictibleEntry<K, V> e);

    /**
     * Removes the specified evictible entry from the queue.
     * 
     * @param e
     *            the entry to be removed from the queue.
     * 
     * @throws NullPointerException
     *             if the entry is <code>null</code>
     */
    public void removeEntry(EvictibleEntry<K, V> e);

    /**
     * Evicts all entries that have expired from their maps and removes them
     * from the queue.
     * 
     * @return <tt>true</tt> if any entries have been evicted
     */
    public boolean evictEntries();
}
