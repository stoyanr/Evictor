package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;

public class SingleRegularTaskEvictionScheduler<K, V> extends
    AbstractSingleTaskEvictionScheduler<K, V> {

    public SingleRegularTaskEvictionScheduler() {
        super();
    }

    public SingleRegularTaskEvictionScheduler(ScheduledExecutorService ses) {
        super(ses);
    }

    @Override
    protected void onScheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        schedule(map);
    }

    @Override
    protected void onCancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        cancel();
    }

    @Override
    protected void onCancelAllEvictions(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        cancel();
    }

    @Override
    protected void onEvictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        cancel();
    }

    private void schedule(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        if (!queue.isEmpty()) {
            scheduleTask(map);
        }
    }

    private void cancel() {
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
