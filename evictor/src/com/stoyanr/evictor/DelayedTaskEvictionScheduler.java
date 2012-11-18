package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class DelayedTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;
    private volatile ScheduledFuture<?> future = null;
    private volatile long next = 0;

    public DelayedTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public DelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
    }

    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService ses) {
        super(queue);
        assert (ses != null);
        this.ses = ses;
    }

    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }

    @Override
    protected void onEvictEntries() {
        schedule();
    }

    private synchronized void scheduleTask() {
        if (future != null) {
            future.cancel(false);
        }
        schedule();
    }

    private synchronized void schedule() {
        next = getNextEvictionTime();
        future = (next > 0) ? ses.schedule(new EvictionRunnable(),
            Math.max(next - System.nanoTime(), 0), NANOSECONDS) : null;
    }

}
