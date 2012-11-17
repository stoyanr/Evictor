package com.stoyanr.evictor;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;

    public ExecutorServiceEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public ExecutorServiceEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
    }

    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        assert (e != null);
        if (e.isEvictible()) {
            ScheduledFuture<?> future = ses.schedule(new EvictionRunnable<K, V>(e),
                Math.max(e.getEvictionTime() - System.nanoTime(), 0), TimeUnit.NANOSECONDS);
            e.setData(future);
        }
    }

    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        assert (e != null);
        ScheduledFuture<?> future = (ScheduledFuture<?>) e.getData();
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
    }

    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    private static final class EvictionRunnable<K, V> implements Runnable {
        private final WeakReference<EvictibleEntry<K, V>> er;

        public EvictionRunnable(EvictibleEntry<K, V> e) {
            assert (e != null);
            er = new WeakReference<EvictibleEntry<K, V>>(e);
        }

        @Override
        public void run() {
            EvictibleEntry<K, V> e = er.get();
            if (e != null) {
                e.evict(false);
            }
        }
    }

}
