package com.stoyanr.concurrent;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConcurrentMapWithTimedEvictionDecorator<K, V> extends AbstractMap<K, V> implements
    ConcurrentMapWithTimedEviction<K, V> {

    private static final int DEFAULT_THREAD_POOL_SIZE = 50;

    private final ConcurrentMap<K, EvictibleEntry<K, V>> delegate;
    private final ConcurrentMap<K, ScheduledFuture<?>> futures;
    private final ScheduledExecutorService ses;
    private transient EntrySet entrySet;

    public ConcurrentMapWithTimedEvictionDecorator() {
        this(new ConcurrentHashMap<K, EvictibleEntry<K, V>>(), new ScheduledThreadPoolExecutor(
            DEFAULT_THREAD_POOL_SIZE));
    }

    public ConcurrentMapWithTimedEvictionDecorator(ConcurrentMap<K, EvictibleEntry<K, V>> delegate,
        ScheduledExecutorService ses) {
        super();
        this.delegate = delegate;
        this.ses = ses;
        this.futures = new ConcurrentHashMap<>();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean containsKey(Object key) {
        assert (key != null);
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? false : true;
    }

    @Override
    public boolean containsValue(Object value) {
        assert (value != null);
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
        assert (key != null);
        EvictibleEntry<K, V> e = delegate.get(key);
        return ((e == null) || evictIfExpired(e)) ? null : e.getValue();
    }

    @Override
    public V put(K key, V value) {
        return put(key, value, 0);
    }

    @Override
    public V put(K key, V value, long evictMs) {
        assert (key != null);
        assert (value != null);
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, value, evictMs);
        EvictibleEntry<K, V> oe = delegate.put(key, e);
        if (oe != null) {
            removeFuture(oe.getKey(), true);
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
        assert (key != null);
        assert (value != null);
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
        assert (key != null);
        EvictibleEntry<K, V> oe = delegate.remove(key);
        if (oe != null) {
            removeFuture(oe.getKey(), true);
        }
        return ((oe == null) || oe.shouldEvict()) ? null : oe.getValue();
    }

    @Override
    public boolean remove(Object key, Object value) {
        assert (key != null);
        assert (value != null);
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oe.getValue().equals(value)) {
            return false;
        } else {
            boolean removed = delegate.remove(key, oe);
            removeFuture(oe.getKey(), true);
            return removed;
        }
    }

    @Override
    public V replace(K key, V value) {
        return replace(key, value, 0);
    }

    @Override
    public V replace(K key, V value, long evictMs) {
        assert (key != null);
        assert (value != null);

        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe)) {
            return null;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, value, evictMs);
        oe = delegate.replace(key, e);
        if (oe != null) {
            removeFuture(oe.getKey(), true);
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
        assert (key != null);
        assert (oldValue != null && newValue != null);

        // Avoid replacing an expired entry
        EvictibleEntry<K, V> oe = delegate.get(key);
        if ((oe == null) || evictIfExpired(oe) || !oldValue.equals(oe.getValue())) {
            return false;
        }

        // Attempt replacement and schedule eviction if successful
        EvictibleEntry<K, V> e = new EvictibleEntry<K, V>(key, newValue, evictMs);
        boolean replaced = delegate.replace(key, oe, e);
        if (replaced) {
            removeFuture(oe.getKey(), true);
            scheduleEviction(e);
        }
        return replaced;
    }

    @Override
    public void clear() {
        delegate.clear();
        removeAllFutures(true);
    }

    @Override
    public Set<K> keySet() {
        return delegate.keySet();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (entrySet == null) {
            entrySet = new EntrySet();
        }
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

    private void evict(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
        delegate.remove(e.getKey(), e);
        removeFuture(e.getKey(), cancelPendingEviction);
    }

    private void scheduleEviction(EvictibleEntry<K, V> e) {
        if (e.getEvictMs() > 0) {
            ScheduledFuture<?> future = ses.schedule(new EvictionRunnable<K, V>(this, e),
                e.getEvictMs(), TimeUnit.MILLISECONDS);
            futures.put(e.getKey(), future);
        }
    }

    private void removeFuture(K key, boolean cancelPendingEviction) {
        ScheduledFuture<?> future = futures.remove(key);
        if (cancelPendingEviction && future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private void removeAllFutures(boolean cancelPendingEviction) {
        for (K key : futures.keySet()) {
            removeFuture(key, cancelPendingEviction);
        }
    }

    private static final class EvictionRunnable<K, V> implements Runnable {
        private final WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>> mr;
        private final WeakReference<EvictibleEntry<K, V>> er;

        public EvictionRunnable(ConcurrentMapWithTimedEvictionDecorator<K, V> m,
            EvictibleEntry<K, V> e) {
            er = new WeakReference<EvictibleEntry<K, V>>(e);
            mr = new WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>>(m);
        }

        public void run() {
            ConcurrentMapWithTimedEvictionDecorator<K, V> map = mr.get();
            EvictibleEntry<K, V> e = er.get();
            System.out.println("EvictionRunnable.run() " + e.toString());
            if ((map != null) && (e != null)) {
                map.evictIfExpired(e, false);
            }
        }
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {

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

    final class EntryIterator implements Iterator<Entry<K, V>> {

        private final Iterator<Entry<K, EvictibleEntry<K, V>>> iterator = delegate.entrySet()
            .iterator();

        @Override
        public Entry<K, V> next() {
            return iterator.next().getValue();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }

}
