package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class RegularTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;
    protected ScheduledFuture<?> future = null;

    public RegularTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public RegularTaskEvictionScheduler(ScheduledExecutorService ses) {
        assert (ses != null);
        this.ses = ses;
    }

    public RegularTaskEvictionScheduler(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public RegularTaskEvictionScheduler(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue, 
        ScheduledExecutorService ses) {
        super(queue);
        assert (ses != null);
        this.ses = ses;
    }

    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        schedule();
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        cancel();
    }

    @Override
    protected void onEvictEntries() {
        cancel();
    }

    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    private void schedule() {
        if (hasScheduledEvictions()) {
            scheduleTask();
        }
    }

    private void cancel() {
        if (!hasScheduledEvictions()) {
            cancelTask();
        }
    }

    private synchronized void scheduleTask() {
        if (future == null) {
            future = ses.scheduleWithFixedDelay(new EvictionRunnable(), 1, 1, MILLISECONDS);
        }
    }

    private synchronized void cancelTask() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

}
