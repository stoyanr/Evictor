package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RegularTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;
    public static final long DEFAULT_DELAY = 1;
    public static final TimeUnit DEFAULT_TIME_UNIT = MILLISECONDS;

    private final ScheduledExecutorService ses;
    private final long delay;
    private final TimeUnit timeUnit;
    private volatile ScheduledFuture<?> future = null;
    private volatile boolean active = false;

    public RegularTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE), DEFAULT_DELAY,
            DEFAULT_TIME_UNIT);
    }

    public RegularTaskEvictionScheduler(ScheduledExecutorService ses, long delay, TimeUnit timeUnit) {
        super();
        assert (ses != null);
        this.ses = ses;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    public RegularTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE), DEFAULT_DELAY,
            DEFAULT_TIME_UNIT);
    }

    public RegularTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService ses,
        long delay, TimeUnit timeUnit) {
        super(queue);
        assert (ses != null);
        this.ses = ses;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (hasScheduledEvictions() && !active) {
            schedule();
        }
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (!hasScheduledEvictions() && active) {
            cancel();
        }
    }

    @Override
    protected void onEvictEntries() {
        if (!hasScheduledEvictions() && active) {
            cancel();
        }
    }

    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    private synchronized void schedule() {
        active = hasScheduledEvictions();
        if (future == null && active) {
            future = ses.scheduleWithFixedDelay(new EvictionRunnable(), delay, delay, timeUnit);
        }
    }

    private synchronized void cancel() {
        active = hasScheduledEvictions();
        if (future != null && !active) {
            future.cancel(false);
            future = null;
        }
    }
}
