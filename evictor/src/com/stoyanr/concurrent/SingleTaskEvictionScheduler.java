package com.stoyanr.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingleTaskEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;
    private final ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue;
    private ScheduledFuture<?> future = null;

    public SingleTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public SingleTaskEvictionScheduler(ScheduledThreadPoolExecutor ses) {
        super();
        assert (ses != null);
        this.ses = ses;
        this.queue = new ConcurrentSkipListMap<>();
    }

    public ScheduledFuture<?> getFuture() {
        return future;
    }

    @Override
    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        if (e.getEvictMs() > 0) {
            queue.put(e.getExpiredNs(), e);
            if (!queue.isEmpty()) {
                scheduleTask(map);
            }
        }
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        if (e.getEvictMs() > 0) {
            queue.remove(e.getExpiredNs(), e);
            if (queue.isEmpty()) {
                cancelTask();
            }
        }
    }

    private synchronized void scheduleTask(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        if (future == null) {
            future = ses.scheduleWithFixedDelay(new EvictionRunnable<K, V>(map, this), 1, 1,
                TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void cancelTask() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }
    
    private void evictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> head = queue.headMap(System.nanoTime());
        if (!head.isEmpty()) {
            for (EvictibleEntry<K, V> e : head.values()) {
                map.evict(e, false);
            }
            head.clear();
            if (queue.isEmpty()) {
                cancelTask();
            }
        }
    }

    private static final class EvictionRunnable<K, V> implements Runnable {
        private final WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>> mr;
        private final WeakReference<SingleTaskEvictionScheduler<K, V>> sr;

        public EvictionRunnable(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
            SingleTaskEvictionScheduler<K, V> scheduler) {
            mr = new WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>>(map);
            sr = new WeakReference<SingleTaskEvictionScheduler<K, V>>(scheduler);
        }

        @Override
        public void run() {
            ConcurrentMapWithTimedEvictionDecorator<K, V> map = mr.get();
            SingleTaskEvictionScheduler<K, V> scheduler = sr.get();
            if (map != null && scheduler != null) {
                scheduler.evictEntries(map);
            }
        }

    }

}
