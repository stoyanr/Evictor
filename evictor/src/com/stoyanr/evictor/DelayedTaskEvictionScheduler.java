package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * A concrete implementation of {@link AbstractQueueEvictionScheduler} which uses a single delayed
 * task scheduled in an {@link java.util.concurrent.ScheduledExecutorService} to manage the
 * automated eviction. The task is rescheduled appropriately each time an entry is added or removed
 * from the queue to ensure that it will always fire at the time of the next scheduled eviction, not
 * sooner or later. If the queue is empty, the task is cancelled until an entry is added.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class DelayedTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;
    private volatile ScheduledFuture<?> future = null;
    private volatile long next = 0;

    /**
     * Creates an eviction scheduler with the default queue implementation and (see
     * {@link AbstractQueueEvictionScheduler}) and a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     */
    public DelayedTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates an eviction scheduler with the default queue implementation and (see
     * {@link AbstractQueueEvictionScheduler}) and the specified executor service.
     */
    public DelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();
        assert (ses != null);
        this.ses = ses;
    }

    /**
     * Creates an eviction scheduler with the specified queue and a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     */
    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates an eviction scheduler with the specified queue and executor service.
     */
    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService ses) {
        super(queue);
        assert (ses != null);
        this.ses = ses;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>shutdownNow</tt> method on the executor service.
     */
    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the current next eviction time is different from the last next
     * eviction time, and if so (re)schedules the task.
     */
    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the current next eviction time is different from the last next
     * eviction time, and if so (re)schedules the task.
     */
    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            scheduleTask();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply always schedules the task.
     */
    @Override
    protected void onEvictEntries() {
        schedule();
    }

    private synchronized void scheduleTask() {
        // Cancel the task if already scheduled, then call schedule() to schedule a new task.
        if (future != null) {
            future.cancel(false);
        }
        schedule();
    }

    private synchronized void schedule() {
        // Get the next eviction time and reschedule the task with a delay corresponding to the
        // difference between this time and the current time. If the next eviction time is 0 
        // (the queue is empty), don't schedule anything.
        next = getNextEvictionTime();
        future = (next > 0) ? ses.schedule(new EvictionRunnable(),
            Math.max(next - System.nanoTime(), 0), NANOSECONDS) : null;
    }

}
