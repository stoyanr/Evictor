package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class SingleRegularTaskEvictionScheduler<K, V> extends
    AbstractSingleTaskEvictionScheduler<K, V> {

    private ScheduledFuture<?> future = null;

    public SingleRegularTaskEvictionScheduler() {
        super();
    }

    public SingleRegularTaskEvictionScheduler(ScheduledExecutorService ses) {
        super(ses);
    }

    @Override
    protected void onScheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        if (!queue.isEmpty()) {
            scheduleTask(map);
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
    protected void onEvictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        if (queue.isEmpty()) {
            cancelTask();
        }
    }

    private synchronized void scheduleTask(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        if (future == null) {
            future = ses.scheduleWithFixedDelay(new EvictionRunnable<K, V>(map, this), 1, 1,
                MILLISECONDS);
        }
    }

    private synchronized void cancelTask() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }
}
