package com.stoyanr.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public abstract class AbstractSingleTaskEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    protected final ScheduledExecutorService ses;
    protected final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue;
    protected ScheduledFuture<?> future = null;

    public AbstractSingleTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public AbstractSingleTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
        this.queue = new ConcurrentSkipListMap<>();
    }

    @Override
    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        assert (map != null);
        assert (e != null);
        if (e.isEvictible()) {
            queue.put(e.getEvictionTime(), e);
            onScheduleEviction(map, e);
        }
    }

    @Override
    public void cancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        assert (map != null);
        assert (e != null);
        if (e.isEvictible()) {
            queue.remove(e.getEvictionTime(), e);
            onCancelEviction(map, e);
        }
    }

    @Override
    public void cancelAllEvictions(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        assert (map != null);
        queue.clear();
        onCancelAllEvictions(map);
    }

    protected abstract void onScheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e);

    protected abstract void onCancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e);

    protected abstract void onCancelAllEvictions(ConcurrentMapWithTimedEvictionDecorator<K, V> map);

    protected abstract void onEvictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map);

    private void evictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = queue.headMap(now());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                map.evict(e, false);
            }
            head.clear();
            onEvictEntries(map);
        }
    }

    protected static final class EvictionRunnable<K, V> implements Runnable {
        private final WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>> mr;
        private final WeakReference<AbstractSingleTaskEvictionScheduler<K, V>> sr;

        public EvictionRunnable(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
            AbstractSingleTaskEvictionScheduler<K, V> scheduler) {
            assert (map != null);
            assert (scheduler != null);
            mr = new WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>>(map);
            sr = new WeakReference<AbstractSingleTaskEvictionScheduler<K, V>>(scheduler);
        }

        @Override
        public void run() {
            ConcurrentMapWithTimedEvictionDecorator<K, V> map = mr.get();
            AbstractSingleTaskEvictionScheduler<K, V> scheduler = sr.get();
            if (map != null && scheduler != null) {
                scheduler.evictEntries(map);
            }
        }

    }

    public static long now() {
        return System.nanoTime();
    }
}
