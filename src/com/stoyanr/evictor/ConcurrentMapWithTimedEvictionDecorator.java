package com.stoyanr.evictor;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ConcurrentMapWithTimedEviction} implementation which decorates an existing
 * {@link java.util.concurrent.ConcurrentMap} implementation. This class uses an instance of
 * {@link EvictionScheduler} for automatically evicting entries upon expiration.
 * 
 * <p>
 * This class does <em>not</em> allow <tt>null</tt> to be used as a value. Whether or not
 * <tt>null</tt> can be used as a key depends on the map being decorated.
 * 
 * <p>
 * This class and its views and iterators implement all of the <em>optional</em> methods of the
 * {@link java.util.Map} and {@link java.util.Iterator} interfaces.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentMapWithTimedEvictionDecorator<K, V> extends AbstractMap<K, V> implements
    ConcurrentMapWithTimedEviction<K, V> {

    private final ConcurrentMap<K, EvictibleEntry<K, V>> delegate;
    private final EvictionScheduler<K, V> scheduler;
    private final transient EntrySet entrySet;

    /**
     * Creates a new map that supports timed entry eviction with the specified delegate and eviction
     * scheduler.
     * 
     * @param delegate the actual map implementation to which all operations will be eventually
     * delegated
     * @param scheduler the scheduler used for automatically evicting entries when the time they are
     * allowed to stay in the map has elapsed
     * @throws NullPointerException if either the delegate or the scheduler is null
     */
    public ConcurrentMapWithTimedEvictionDecorator(ConcurrentMap<K, EvictibleEntry<K, V>> delegate,
        EvictionScheduler<K, V> scheduler) {
        super();
        if (delegate == null || scheduler == null)
            throw new NullPointerException();
        this.delegate = delegate;
        this.scheduler = scheduler;
        this.entrySet = new EntrySet();
    }

    /**
     * Returns the number of key-value mappings in this map. If the map contains more than
     * <tt>Integer.MAX_VALUE</tt> elements, returns <tt>Integer.MAX_VALUE</tt>. This may include
     * entries may not have yet been evicted, but since it is a {@link ConcurrentMap}, it is ok to
     * return a weakly consistent value.
     * 
     * @return the number of key-value mappings in this map
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key and it has not
     * expired yet. More formally, returns <tt>true</tt> if and only if this map contains a mapping
     * for a key <tt>k</tt> such that <tt>(key==null ? k==null : key.equals(k))</tt>. (There can be
     * at most one such mapping.).
     * 
     * @param key key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key and it has not
     * expired yet.
     * @throws ClassCastException if the key is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys
     */
    @Override
    public boolean containsKey(Object key) {
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? false : true;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value and at least
     * one of them has not expired yet. More formally, returns <tt>true</tt> if and only if this map
     * contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>.
     * 
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the specified value and at least
     * one of them has not expired yet.
     * @throws ClassCastException if the value is of an inappropriate type for this map
     * @throws NullPointerException if the specified value is null
     */
    @Override
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        for (EvictibleEntry<K, V> e : delegate.values()) {
            if (e.getValue().equals(value)) {
                if (evictIfExpired(e)) {
                    continue;
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key or it has expired.
     * 
     * <p>
     * If the delegate map permits null values, then a return value of {@code null} does not
     * <i>necessarily</i> indicate that the map contains no mapping for the key; it's also possible
     * that the map explicitly maps the key to {@code null}. The {@link #containsKey containsKey}
     * operation may be used to distinguish these two cases.
     * 
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains
     * no mapping for the key or it has expired
     * @throws ClassCastException if the key is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys
     */
    @Override
    public V get(Object key) {
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? null : e.getValue();
    }

    /**
     * Associates the specified value with the specified key in this map. If the map previously
     * contained a mapping for the key, the old value is replaced by the specified value.
     * 
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no
     * mapping for <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with <tt>key</tt>, if the delegate supports <tt>null</tt> values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map
     */
    @Override
    public V put(K key, V value) {
        return put(key, value, 0);
    }

    /**
     * Associates the specified value with the specified key in this map for the specified duration.
     * If the map previously contained a mapping for the key, the old value is replaced by the
     * specified value. (A map <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and
     * only if {@link #containsKey(Object) m.containsKey(k)} would return <tt>true</tt>.)
     * 
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param evictMs the time in ms during which the entry can stay in the map (time-to-live). When
     * this time has elapsed, the entry will be evicted from the map automatically. A value of 0 for
     * this argument means "forever", i.e. <tt>put(key, value, 0)</tt> is equivalent to
     * <tt>put(key, value)</tt>.
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no
     * mapping for <tt>key</tt>. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with <tt>key</tt>, if the implementation supports <tt>null</tt>
     * values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map, or if evictMs is negative
     */
    @Override
    public V put(K key, V value, long evictMs) {
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
        EvictibleEntry<K, V> oe = delegate.put(key, e);
        if (oe != null) {
            // An entry is being removed, cancel its automatic eviction
            cancelEviction(oe);
        }
        scheduleEviction(e);
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    /**
     * If the specified key is not already associated with a value, associate it with the given
     * value. This is equivalent to
     * 
     * <pre>
     * if (!map.containsKey(key))
     *     return map.put(key, value);
     * else
     *     return map.get(key);
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or <tt>null</tt> if there was
     * no mapping for the key. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with the key, if the implementation supports null values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map
     * 
     */
    @Override
    public V putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, 0);
    }

    /**
     * If the specified key is not already associated with a value, associate it with the given
     * value for the specified duration. This is equivalent to
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
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @param evictMs the time in ms during which the entry can stay in the map (time-to-live). When
     * this time has elapsed, the entry will be evicted from the map automatically. A value of 0 for
     * this argument means "forever", i.e. <tt>putIfAbsent(key, value, 0)</tt> is equivalent to
     * <tt>putIfAbsent(key, value)</tt>.
     * @return the previous value associated with the specified key, or <tt>null</tt> if there was
     * no mapping for the key. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with the key, if the implementation supports null values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map, or if evictMs is negative
     */
    @Override
    public V putIfAbsent(K key, V value, long evictMs) {
        while (true) {
            EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
            EvictibleEntry<K, V> oe = delegate.putIfAbsent(key, e);
            if (oe == null) {
                // An entry is being added, schedule its automatic eviction
                scheduleEviction(e);
                return null;
            } else if (evictIfExpired(oe)) {
                continue;
            } else {
                return oe.getValue();
            }
        }
    }

    /**
     * Removes the mapping for a key from this map if it is present. More formally, if this map
     * contains a mapping from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping is removed. (The map can
     * contain at most one such mapping.)
     * 
     * <p>
     * Returns the value to which this map previously associated the key, or <tt>null</tt> if the
     * map contained no mapping for the key or it has expired.
     * 
     * <p>
     * If the delegate map permits null values, then a return value of <tt>null</tt> does not
     * <i>necessarily</i> indicate that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to <tt>null</tt>.
     * 
     * <p>
     * The map will not contain a mapping for the specified key once the call returns.
     * 
     * @param key key whose mapping is to be removed from the map, if it has not expired yet.
     * @return the previous value associated with <tt>key</tt>, or <tt>null</tt> if there was no
     * mapping for <tt>key</tt> or it has expired.
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by
     * the delegate map
     * @throws ClassCastException if the key is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys
     */
    @Override
    public V remove(Object key) {
        EvictibleEntry<K, V> oe = delegate.remove(key);
        if (oe != null) {
            // An entry is being removed, cancel its automatic eviction
            cancelEviction(oe);
        }
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    /**
     * Removes the entry for a key only if currently mapped to a given value. This is equivalent to
     * 
     * <pre>
     * if (map.containsKey(key) &amp;&amp; map.get(key).equals(value)) {
     *     map.remove(key);
     *     return true;
     * } else
     *     return false;
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return <tt>true</tt> if the value was removed without being expired
     * @throws UnsupportedOperationException if the <tt>remove</tt> operation is not supported by
     * the delegate map
     * @throws ClassCastException if the key or value is of an inappropriate type for this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     */
    @Override
    public boolean remove(Object key, Object value) {
        if (value == null)
            throw new NullPointerException();
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oe.getValue().equals(value)) {
            return false;
        } else {
            boolean removed = delegate.remove(key, oe);
            // An entry is being removed, cancel its automatic eviction
            cancelEviction(oe);
            return removed;
        }
    }

    /**
     * Replaces the entry for a key only if currently mapped to some value. This is equivalent to
     * 
     * <pre>
     * if (map.containsKey(key)) {
     *     return map.put(key, value);
     * } else
     *     return null;
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or <tt>null</tt> if there was
     * no mapping for the key. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with the key, if the delegate map supports null values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map
     */
    @Override
    public V replace(K key, V value) {
        return replace(key, value, 0);
    }

    /**
     * Replaces the entry for a key only if currently mapped to some value, for the specified
     * duration. This is equivalent to
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
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @param evictMs the time in ms during which the entry can stay in the map (time-to-live). When
     * this time has elapsed, the entry will be evicted from the map automatically. A value of 0 for
     * this argument means "forever", i.e. <tt>replace(key, value, 0)</tt> is equivalent to
     * <tt>replace(key, value)</tt>.
     * @return the previous value associated with the specified key, or <tt>null</tt> if there was
     * no mapping for the key. (A <tt>null</tt> return can also indicate that the map previously
     * associated <tt>null</tt> with the key, if the delegate map supports null values.)
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of the specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of the specified key or value prevents it
     * from being stored in this map, or if evictMs is negative
     */
    @Override
    public V replace(K key, V value, long evictMs) {
        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe)) {
            return null;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, value, evictMs);
        oe = delegate.replace(key, e);
        if (oe != null) {
            // An entry is being replaced, cancel the automatic eviction of the old entry 
            // and schedule it for the new entry 
            cancelEviction(oe);
            scheduleEviction(e);
        }
        return (oe != null) ? oe.getValue() : null;
    }

    /**
     * Replaces the entry for a key only if currently mapped to a given value. This is equivalent to
     * 
     * <pre>
     * if (map.containsKey(key) &amp;&amp; map.get(key).equals(oldValue)) {
     *     map.put(key, newValue);
     *     return true;
     * } else
     *     return false;
     * </pre>
     * 
     * except that the action is performed atomically.
     * 
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return <tt>true</tt> if the value was replaced
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of a specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of a specified key or value prevents it
     * from being stored in this map
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return replace(key, oldValue, newValue, 0);
    }

    /**
     * Replaces the entry for a key only if currently mapped to a given value, for the specified
     * duration. This is equivalent to
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
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @param evictMs the time in ms during which the entry can stay in the map (time-to-live). When
     * this time has elapsed, the entry will be evicted from the map automatically. A value of 0 for
     * this argument means "forever", i.e. <tt>replace(key, oldValue, newValue, 0)</tt> is
     * equivalent to <tt>put(key, oldValue, newValue)</tt>.
     * @return <tt>true</tt> if the value was replaced
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is not supported by the
     * delegate map
     * @throws ClassCastException if the class of a specified key or value prevents it from being
     * stored in this map
     * @throws NullPointerException if the specified key is null and the delegate map does not
     * permit null keys, or if the specified value is null
     * @throws IllegalArgumentException if some property of a specified key or value prevents it
     * from being stored in this map, or if evictMs is negative
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue, long evictMs) {
        if (oldValue == null)
            throw new NullPointerException();
        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oldValue.equals(oe.getValue())) {
            return false;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(this, key, newValue, evictMs);
        boolean replaced = delegate.replace(key, oe, e);
        if (replaced) {
            // An entry is being replaced, cancel the automatic eviction of the old entry 
            // and schedule it for the new entry 
            cancelEviction(oe);
            scheduleEviction(e);
        }
        return replaced;
    }

    /**
     * Removes all of the mappings from this map. The map will be empty after this call returns.
     * 
     * @throws UnsupportedOperationException if the <tt>clear</tt> operation is not supported by the
     * delegate map
     */
    @Override
    public void clear() {
        cancelAllEvictions();
        delegate.clear();
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map. The set is backed by the map,
     * so changes to the map are reflected in the set, and vice-versa. If the map is modified while
     * an iteration over the set is in progress (except through the iterator's own <tt>remove</tt>
     * operation), the results of the iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the <tt>add</tt> or <tt>addAll</tt> operations.
     * 
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map. The set is backed by the
     * map, so changes to the map are reflected in the set, and vice-versa. If the map is modified
     * while an iteration over the set is in progress (except through the iterator's own
     * <tt>remove</tt> operation, or through the <tt>setValue</tt> operation on a map entry returned
     * by the iterator) the results of the iteration are undefined. The set supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations. It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     * 
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    private boolean evictIfExpired(EvictibleEntry<K, V> e) {
        return evictIfExpired(e, true);
    }

    /*
     * Removes the entry from the map if it has already expired, and optionally cancels its
     * automatic eviction.
     */
    private boolean evictIfExpired(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        boolean result = e.shouldEvict();
        if (result) {
            evict(e, cancelPendingEviction);
        }
        return result;
    }

    /*
     * Removes the entry from the map and optionally cancels its automatic eviction.
     */
    void evict(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        delegate.remove(e.getKey(), e);
        if (cancelPendingEviction) {
            cancelEviction(e);
        }
    }

    /*
     * Schedules the automatic eviction for the entry. This method is invoked on new entries that
     * have just been added to the map.
     */
    private void scheduleEviction(EvictibleEntry<K, V> e) {
        scheduler.scheduleEviction(e);
    }

    /*
     * Cancels the automatic eviction for the entry. This method is invoked on old entries that have
     * just been removed from the map.
     */
    private void cancelEviction(EvictibleEntry<K, V> e) {
        scheduler.cancelEviction(e);
    }

    /*
     * Cancels all pending evictions. This method is invoked when the map is cleared.
     */
    private void cancelAllEvictions() {
        for (EvictibleEntry<K, V> e : delegate.values()) {
            scheduler.cancelEviction(e);
        }
    }

    /*
     * An entry set view on this map.
     */
    private final class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?>))
                return false;
            Entry<K, V> e = (Entry<K, V>) o;
            V value = get(e.getKey());
            return (value != null) && value.equals(e.getValue());
        }

        @Override
        public boolean add(Entry<K, V> entry) {
            return (putIfAbsent(entry.getKey(), entry.getValue()) == null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Entry<?, ?>))
                return false;
            Entry<K, V> e = (Entry<K, V>) o;
            return ConcurrentMapWithTimedEvictionDecorator.this.remove(e.getKey(), e.getValue());
        }

        @Override
        public int size() {
            return ConcurrentMapWithTimedEvictionDecorator.this.size();
        }

        @Override
        public boolean isEmpty() {
            return (size() == 0);
        }

        @Override
        public void clear() {
            ConcurrentMapWithTimedEvictionDecorator.this.clear();
        }
    }

    /*
     * An iterator for this map.
     */
    private final class EntryIterator implements Iterator<Entry<K, V>> {

        private final Iterator<Entry<K, EvictibleEntry<K, V>>> iterator = delegate.entrySet()
            .iterator();
        private volatile Entry<K, V> ce = null;

        public EntryIterator() {
        }

        @Override
        public Entry<K, V> next() {
            ce = iterator.next().getValue();
            return ce;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public synchronized void remove() {
            if (ce != null) {
                ConcurrentMapWithTimedEvictionDecorator.this.remove(ce.getKey(), ce.getValue());
                ce = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }

}
