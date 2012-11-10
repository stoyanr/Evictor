package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class SingleDelayedTaskEvictionScheduler<K, V> extends
    AbstractSingleTaskEvictionScheduler<K, V> {

    private ScheduledFuture<?> future = null;

    public SingleDelayedTaskEvictionScheduler() {
        super();
    }

    public SingleDelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super(ses);
    }

    @Override
    protected void onScheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        if (!queue.isEmpty()) {
            scheduleTask(map, e.getEvictionTime());
        }
    }

    @Override
    protected void onCancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        if (queue.isEmpty()) {
            cancelTask();
        }
    }

    @Override
    protected synchronized void onEvictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        if (!queue.isEmpty()) {
            future = scheduleAt(map, queue.firstKey());
        } else {
            future = null;
        }
    }

    private synchronized void scheduleTask(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        long time) {
        if (future == null) {
            future = scheduleAt(map, time);
        } else if (!queue.isEmpty() && (queue.firstKey() == time)) {
            future.cancel(false);
            future = scheduleAt(map, time);
        }
    }

    private synchronized void cancelTask() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private ScheduledFuture<?> scheduleAt(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        long time) {
        return ses.schedule(new EvictionRunnable<K, V>(map, this), Math.max(time - now(), 0),
            NANOSECONDS);
    }
}
