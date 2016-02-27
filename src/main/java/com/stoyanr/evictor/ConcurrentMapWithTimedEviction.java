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

import java.util.concurrent.ConcurrentMap;

/**
 * A {@link java.util.concurrent.ConcurrentMap} that supports timed entry
 * eviction. It provides overloaded <tt>put</tt>, <tt>putIfAbsent</tt>, and
 * <tt>replace</tt> methods with one additional parameter, <tt>evictMs</tt>,
 * which is the time in ms during which the entry can stay in the map
 * (time-to-live).
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public interface ConcurrentMapWithTimedEviction<K, V> extends ConcurrentMap<K, V> {

    /**
     * Associates the specified value with the specified key in this map for the
     * specified duration (optional operation). If the map previously contained
     * a mapping for the key, the old value is replaced by the specified value.
     * (A map <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if
     * and only if {@link #containsKey(Object) m.containsKey(k)} would return
     * <tt>true</tt>.)
     * 
     * @param key
     *            key with which the specified value is to be associated
     * 
     * @param value
     *            value to be associated with the specified key
     * 
     * @param evictMs
     *            the time in ms during which the entry can stay in the map
     *            (time-to-live). When this time has elapsed, the entry will be
     *            evicted from the map automatically. A value of 0 for this
     *            argument means "forever", i.e. <tt>put(key, value, 0)</tt> is
     *            equivalent to <tt>put(key, value).
     * 
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt>
     *         if there was no mapping for <tt>key</tt>. (A <tt>null</tt> return
     *         can also indicate that the map previously associated
     *         <tt>null</tt> with <tt>key</tt>, if the implementation supports
     *         <tt>null</tt> values.)
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by this map
     * 
     * @throws ClassCastException
     *             if the class of the specified key or value prevents it from
     *             being stored in this map
     * 
     * @throws NullPointerException
     *             if the specified key or value is null and this map does not
     *             permit null keys or values
     * 
     * @throws IllegalArgumentException
     *             if some property of the specified key or value prevents it
     *             from being stored in this map, or if evictMs is negative
     */
    public V put(K key, V value, long evictMs);

    /**
     * If the specified key is not already associated with a value, associate it
     * with the given value for the specified duration. This is equivalent to
     * 
     * <pre>
     * if (!map.containsKey(key))
     *     return map.put(key, value, evictMs);
     * else
     *     return map.get(key);
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key
     *            key with which the specified value is to be associated
     * 
     * @param value
     *            value to be associated with the specified key
     * 
     * @param evictMs
     *            the time in ms during which the entry can stay in the map
     *            (time-to-live). When this time has elapsed, the entry will be
     *            evicted from the map automatically. A value of 0 for this
     *            argument means "forever", i.e.
     *            <tt>putIfAbsent(key, value, 0)</tt> is equivalent to
     *            <tt>putIfAbsent(key, value).
     * 
     * @return the previous value associated with the specified key, or <tt>null</tt>
     *         if there was no mapping for the key. (A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the key, if the implementation supports null values.)
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by this map
     * 
     * @throws ClassCastException
     *             if the class of the specified key or value prevents it from
     *             being stored in this map
     * 
     * @throws NullPointerException
     *             if the specified key or value is null, and this map does not
     *             permit null keys or values
     * 
     * @throws IllegalArgumentException
     *             if some property of the specified key or value prevents it
     *             from being stored in this map, or if evictMs is negative
     */
    public V putIfAbsent(K key, V value, long evictMs);

    /**
     * Replaces the entry for a key only if currently mapped to some value, for
     * the specified duration. This is equivalent to
     * 
     * <pre>
     * if (map.containsKey(key)) {
     *     return map.put(key, value, evictMs);
     * } else
     *     return null;
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key
     *            key with which the specified value is associated
     * 
     * @param value
     *            value to be associated with the specified key
     * 
     * @param evictMs
     *            the time in ms during which the entry can stay in the map
     *            (time-to-live). When this time has elapsed, the entry will be
     *            evicted from the map automatically. A value of 0 for this
     *            argument means "forever", i.e. <tt>replace(key, value, 0)</tt>
     *            is equivalent to <tt>replace(key, value).
     * 
     * @return the previous value associated with the specified key, or <tt>null</tt>
     *         if there was no mapping for the key. (A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the key, if the implementation supports null values.)
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by this map
     * 
     * @throws ClassCastException
     *             if the class of the specified key or value prevents it from
     *             being stored in this map
     * 
     * @throws NullPointerException
     *             if the specified key or value is null, and this map does not
     *             permit null keys or values
     * 
     * @throws IllegalArgumentException
     *             if some property of the specified key or value prevents it
     *             from being stored in this map, or if evictMs is negative
     */
    public V replace(K key, V value, long evictMs);

    /**
     * Replaces the entry for a key only if currently mapped to a given value,
     * for the specified duration. This is equivalent to
     * 
     * <pre>
     * if (map.containsKey(key) &amp;&amp; map.get(key).equals(oldValue)) {
     *     map.put(key, newValue, evictMs);
     *     return true;
     * } else
     *     return false;
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key
     *            key with which the specified value is associated
     * 
     * @param oldValue
     *            value expected to be associated with the specified key
     * 
     * @param newValue
     *            value to be associated with the specified key
     * 
     * @param evictMs
     *            the time in ms during which the entry can stay in the map
     *            (time-to-live). When this time has elapsed, the entry will be
     *            evicted from the map automatically. A value of 0 for this
     *            argument means "forever", i.e.
     *            <tt>replace(key, oldValue, newValue, 0)</tt> is equivalent to
     *            <tt>put(key, oldValue, newValue).
     * 
     * @return <tt>true</tt> if the value was replaced
     * 
     * @throws UnsupportedOperationException
     *             if the <tt>put</tt> operation is not supported by this map
     * 
     * @throws ClassCastException
     *             if the class of a specified key or value prevents it from
     *             being stored in this map
     * 
     * @throws NullPointerException
     *             if a specified key or value is null, and this map does not
     *             permit null keys or values
     * 
     * @throws IllegalArgumentException
     *             if some property of a specified key or value prevents it from
     *             being stored in this map, or if evictMs is negative
     */
    public boolean replace(K key, V oldValue, V newValue, long evictMs);
}
