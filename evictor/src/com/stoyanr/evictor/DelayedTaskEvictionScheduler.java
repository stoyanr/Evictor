package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class DelayedTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;
    private ScheduledFuture<?> future = null;

    public DelayedTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public DelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
    }

    public DelayedTaskEvictionScheduler(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    public DelayedTaskEvictionScheduler(ConcurrentNavigableMap<Long, EvictibleEntry<K, V>> queue, 
        ScheduledExecutorService ses) {
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
        if (hasScheduledEvictions()) {
            scheduleTask(e.getEvictionTime());
        }
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (!hasScheduledEvictions()) {
            cancelTask();
        } else {
            scheduleTaskOnCancel(e.getEvictionTime());
        }
    }

    @Override
    protected void onEvictEntries() {
        if (hasScheduledEvictions()) {
            future = scheduleAt(getNextEvictionTime());
        } else {
            future = null;
        }
    }

    private synchronized void scheduleTask(long time) {
        if (future == null) {
            future = scheduleAt(time);
        } else if (getNextEvictionTime() == time) {
            future.cancel(false);
            future = scheduleAt(time);
        }
    }

    private synchronized void scheduleTaskOnCancel(long time) {
        if (future != null) {
            long next = getNextEvictionTime();
            if (next > time) {
                future.cancel(false);
                future = scheduleAt(next);
            }
        }
    }

    private synchronized void cancelTask() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private ScheduledFuture<?> scheduleAt(long time) {
        return (time > 0) ? ses.schedule(new EvictionRunnable(),
            Math.max(time - System.nanoTime(), 0), NANOSECONDS) : null;
    }

}
