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

package com.stoyanr.evictor.map;

import java.util.concurrent.ConcurrentHashMap;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;
import com.stoyanr.evictor.EvictionScheduler;
import com.stoyanr.evictor.scheduler.SingleThreadEvictionScheduler;

/**
 * A {@link ConcurrentMapWithTimedEviction} which conforms to the
 * {@link java.util.concurrent.ConcurrentHashMap} specification. This class is a
 * simple extension of {@link ConcurrentMapWithTimedEvictionDecorator} which
 * always uses an instance of {@link java.util.concurrent.ConcurrentHashMap} as
 * a delegate. For convenience, it provides a number of overloaded constructors
 * which correspond to the constructors provided by the
 * {@link java.util.concurrent.ConcurrentHashMap} class.
 * 
 * <p>
 * Like {@link java.util.concurrent.ConcurrentHashMap}, this class does
 * <em>not</em> allow <tt>null</tt> to be used as a key or value.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class ConcurrentHashMapWithTimedEviction<K, V> extends ConcurrentMapWithTimedEvictionDecorator<K, V> implements ConcurrentMapWithTimedEviction<K, V> {

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity, load factor, concurrency level, and eviction
     * scheduler.
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @param loadFactor
     *            the load factor threshold, used to control resizing. Resizing
     *            may be performed when the average number of elements per bin
     *            exceeds this threshold.
     * 
     * @param concurrencyLevel
     *            the estimated number of concurrently updating threads. The
     *            implementation performs internal sizing to try to accommodate
     *            this many threads.
     * 
     * @param scheduler
     *            the scheduler used for automatically evicting entries when the
     *            time they are allowed to stay in the map has elapsed.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity is negative or the load factor or
     *             concurrencyLevel are non-positive.
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, int concurrencyLevel, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor, concurrencyLevel), scheduler);
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity, load factor, and concurrency level, and with
     * the default eviction scheduler.
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @param loadFactor
     *            the load factor threshold, used to control resizing. Resizing
     *            may be performed when the average number of elements per bin
     *            exceeds this threshold.
     * 
     * @param concurrencyLevel
     *            the estimated number of concurrently updating threads. The
     *            implementation performs internal sizing to try to accommodate
     *            this many threads.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity is negative or the load factor or
     *             concurrencyLevel are non-positive.
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, int concurrencyLevel) {
        this(initialCapacity, loadFactor, concurrencyLevel, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity, load factor and eviction scheduler, and with
     * the default concurrencyLevel (16).
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @param loadFactor
     *            the load factor threshold, used to control resizing. Resizing
     *            may be performed when the average number of elements per bin
     *            exceeds this threshold.
     * 
     * @param scheduler
     *            the scheduler used for automatically evicting entries when the
     *            time they are allowed to stay in the map has elapsed.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity of elements is negative or the load
     *             factor is non-positive
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity, loadFactor), scheduler);
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity and load factor, and with the default
     * concurrencyLevel (16) and eviction scheduler.
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @param loadFactor
     *            the load factor threshold, used to control resizing. Resizing
     *            may be performed when the average number of elements per bin
     *            exceeds this threshold.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity of elements is negative or the load
     *             factor is non-positive
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity and eviction scheduler, and with the default
     * load factor (0.75) and concurrencyLevel (16).
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @param scheduler
     *            the scheduler used for automatically evicting entries when the
     *            time they are allowed to stay in the map has elapsed.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity of elements is negative
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity, EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(initialCapacity), scheduler);
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified initial capacity, and with the default load factor (0.75),
     * concurrencyLevel (16), and eviction scheduler.
     * 
     * @param initialCapacity
     *            the initial capacity. The implementation performs internal
     *            sizing to accommodate this many elements.
     * 
     * @throws IllegalArgumentException
     *             if the initial capacity of elements is negative
     */
    public ConcurrentHashMapWithTimedEviction(int initialCapacity) {
        this(initialCapacity, ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with the
     * specified eviction scheduler, and with the default, initial capacity
     * (16), load factor (0.75) and concurrencyLevel (16).
     * 
     * @param scheduler
     *            the scheduler used for automatically evicting entries when the
     *            time they are allowed to stay in the map has elapsed.
     */
    public ConcurrentHashMapWithTimedEviction(EvictionScheduler<K, V> scheduler) {
        super(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(), scheduler);
    }

    /**
     * Creates a new, empty map that supports timed entry eviction with a
     * default initial capacity (16), load factor (0.75), concurrencyLevel (16),
     * and eviction scheduler.
     */
    public ConcurrentHashMapWithTimedEviction() {
        this(ConcurrentHashMapWithTimedEviction.<K, V> defaultScheduler());
    }

    private static <K, V> EvictionScheduler<K, V> defaultScheduler() {
        return new SingleThreadEvictionScheduler<K, V>();
    }
}
