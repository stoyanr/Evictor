package com.stoyanr.concurrent;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiTaskEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;

    public MultiTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public MultiTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
    }

    @Override
    public void scheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        if (e.getEvictMs() > 0) {
            ScheduledFuture<?> future = ses.schedule(new EvictionRunnable<K, V>(map, e),
                e.getEvictMs(), TimeUnit.MILLISECONDS);
            e.setData(future);
        }
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        ScheduledFuture<?> future = (ScheduledFuture<?>) e.getData();
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    private static final class EvictionRunnable<K, V> implements Runnable {
        private final WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>> mr;
        private final WeakReference<EvictibleEntry<K, V>> er;

        public EvictionRunnable(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
            EvictibleEntry<K, V> e) {
            mr = new WeakReference<ConcurrentMapWithTimedEvictionDecorator<K, V>>(map);
            er = new WeakReference<EvictibleEntry<K, V>>(e);
        }

        @Override
        public void run() {
            ConcurrentMapWithTimedEvictionDecorator<K, V> map = mr.get();
            EvictibleEntry<K, V> e = er.get();
            if (map != null && e != null) {
                map.evict(e, false);
            }
        }
    }

}
