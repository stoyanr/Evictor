package com.stoyanr.evictor;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * A {@link ConcurrentMapWithTimedEviction} implementation which decorates an existing
 * {@link java.util.concurrent.ConcurrentMap} implementation. This class uses an instance of
 * {@link EvictionScheduler} for automatically evicting entries when the time they are allowed to
 * stay in the map has elapsed.
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
     * delegated.
     * @param scheduler the scheduler used for automatically evicting entries when the time they are
     * allowed to stay in the map has elapsed.
     */
    public ConcurrentMapWithTimedEvictionDecorator(ConcurrentMap<K, EvictibleEntry<K, V>> delegate,
        EvictionScheduler<K, V> scheduler) {
        super();
        assert (delegate != null);
        assert (scheduler != null);
        this.delegate = delegate;
        this.scheduler = scheduler;
        this.entrySet = new EntrySet();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean containsKey(Object key) {
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? false : true;
    }

    @Override
    public boolean containsValue(Object value) {
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

    @Override
    public V get(Object key) {
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? null : e.getValue();
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, 0);
    }

    @Override
    public V put(K key, V value, long evictMs) {
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, value, evictMs);
        EvictibleEntry<K, V> oe = delegate.put(key, e);
        if (oe != null) {
            cancelEviction(oe);
        }
        scheduleEviction(e);
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        return putIfAbsent(key, value, 0);
    }

    @Override
    public V putIfAbsent(K key, V value, long evictMs) {
        while (true) {
            EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, value, evictMs);
            EvictibleEntry<K, V> oe = delegate.putIfAbsent(key, e);
            if (oe == null) {
                scheduleEviction(e);
                return null;
            } else if (evictIfExpired(oe)) {
                continue;
            } else {
                return oe.getValue();
            }
        }
    }

    @Override
    public V remove(Object key) {
        EvictibleEntry<K, V> oe = delegate.remove(key);
        if (oe != null) {
            cancelEviction(oe);
        }
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    @Override
    public boolean remove(Object key, Object value) {
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oe.getValue().equals(value)) {
            return false;
        } else {
            boolean removed = delegate.remove(key, oe);
            cancelEviction(oe);
            return removed;
        }
    }

    @Override
    public V replace(K key, V value) {
        return replace(key, value, 0);
    }

    @Override
    public V replace(K key, V value, long evictMs) {
        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe)) {
            return null;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, value, evictMs);
        oe = delegate.replace(key, e);
        if (oe != null) {
            cancelEviction(oe);
            scheduleEviction(e);
        }
        return (oe != null) ? oe.getValue() : null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return replace(key, oldValue, newValue, 0);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue, long evictMs) {
        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oldValue.equals(oe.getValue())) {
            return false;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, newValue, evictMs);
        boolean replaced = delegate.replace(key, oe, e);
        if (replaced) {
            cancelEviction(oe);
            scheduleEviction(e);
        }
        return replaced;
    }

    @Override
    public void clear() {
        cancelAllEvictions();
        delegate.clear();
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    private boolean evictIfExpired(EvictibleEntry<K, V> e) {
        return evictIfExpired(e, true);
    }

    private boolean evictIfExpired(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        boolean result = e.shouldEvict();
        if (result) {
            evict(e, cancelPendingEviction);
        }
        return result;
    }

    void evict(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        delegate.remove(e.getKey(), e);
        if (cancelPendingEviction) {
            cancelEviction(e);
        }
    }

    private void scheduleEviction(EvictibleEntry<K, V> e) {
        scheduler.scheduleEviction(this, e);
    }

    private void cancelEviction(EvictibleEntry<K, V> e) {
        scheduler.cancelEviction(this, e);
    }

    private void cancelAllEvictions() {
        scheduler.cancelAllEvictions(this);
    }

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
