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

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import com.stoyanr.evictor.EvictionQueue;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * An {@link EvictionQueue} which uses a {@link java.util.Queue} to store its
 * entries. The queue should support priority queue semantics and the comparator
 * {@link PriorityEvictionQueue.EvictibleEntryComparator} should always be used
 * for comparing entries.
 * 
 * <p>
 * The interface used is {@link java.util.Queue} and not
 * {@link java.util.PriorityQueue} since the most obvious concurrent
 * implementation, {@link PriorityBlockingQueue} only conforms to the former
 * interface. This allows passing a different queue implementation, as far as it
 * is concurrent (thread-safe) and has priority semantics.
 * </p>
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class PriorityEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final Queue<EvictibleEntry<K, V>> queue;

    /**
     * Creates a priority eviction queue with a
     * {@link java.util.concurrent.PriorityBlockingQueue} with the specified
     * initial capacity.
     * 
     * @param initialCapacity
     *            the initial capacity
     * 
     * @throws IllegalArgumentException
     *             if initial capacity is less than 1
     */
    public PriorityEvictionQueue(int initialCapacity) {
        this(new PriorityBlockingQueue<EvictibleEntry<K, V>>(initialCapacity, new EvictibleEntryComparator<K, V>()));
    }

    /**
     * Creates a priority eviction queue with the specified queue.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @throws NullPointerException
     *             if the queue is <code>null</code>
     */
    public PriorityEvictionQueue(Queue<EvictibleEntry<K, V>> queue) {
        if (queue == null) {
            throw new NullPointerException("Queue instance cannot be null");
        }

        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns true if the queue is non-empty and
     * vice versa.
     * </p>
     */
    @Override
    public boolean hasEntries() {
        return !this.queue.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns the eviction time of the first entry
     * in the queue if it is non-empty, or 0 otherwise.
     * </p>
     */
    @Override
    public long getNextEvictionTime() {
        try {
            return (!this.queue.isEmpty()) ? this.queue.peek().getEvictionTime() : 0;
        } catch (NullPointerException e) {
            // Safeguard in a concurrent environment
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>add</tt> method on the queue.
     * </p>
     */
    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        this.queue.add(e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>remove</tt> method on the
     * queue.
     * </p>
     */
    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        this.queue.remove(e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation uses the <tt>peek</tt> and <tt>poll</tt> method on
     * the queue repeatedly to find and remove all entries that should be
     * evicted, evicting them via their <tt>evict</tt> method.
     * </p>
     */
    @Override
    public boolean evictEntries() {
        boolean result = false;
        try {
            while (!this.queue.isEmpty() && (this.queue.peek().getEvictionTime() < System.nanoTime())) {
                this.queue.poll().evict(false);
                result = true;
            }
        } catch (NullPointerException e) {
            // Safeguard in a concurrent environment
        }
        return result;
    }

    /**
     * A comparator that compares {@link EvictibleEntry} instances based on
     * their eviction time.
     * 
     * @author Stoyan Rachev
     * 
     * @param <K>
     *            the type of keys maintained by this map
     * 
     * @param <V>
     *            the type of mapped values
     * 
     */
    public static class EvictibleEntryComparator<K, V> implements Comparator<EvictibleEntry<K, V>> {

        @Override
        public int compare(EvictibleEntry<K, V> entry1, EvictibleEntry<K, V> entry2) {
            long time1 = entry1.getEvictionTime(), time2 = entry2.getEvictionTime();
            if (time1 > time2) {
                return 1;
            }

            if (time1 < time2) {
                return -1;
            }

            return 0;
        }
    }

}
