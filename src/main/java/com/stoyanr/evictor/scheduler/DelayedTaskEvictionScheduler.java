/**
 * 
 * Copyright 2012, Stoyan Rachev
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stoyanr.evictor.scheduler;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.stoyanr.evictor.EvictionQueue;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * A concrete implementation of {@link AbstractQueueEvictionScheduler} which
 * uses a single delayed task scheduled in an
 * {@link java.util.concurrent.ScheduledExecutorService} to manage the automated
 * eviction. The task is rescheduled appropriately each time an entry is added
 * or removed from the queue to ensure that it will always fire at the time of
 * the next scheduled eviction, not sooner or later. If the queue is empty, the
 * task is cancelled until an entry is added.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class DelayedTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;

    private volatile ScheduledFuture<?> future = null;

    private volatile long next = 0;

    /**
     * Creates a delayed task eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}) and a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     */
    public DelayedTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates a delayed task eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}) and the
     * specified scheduled executor service.
     * 
     * @param ses
     *            the scheduled executor service to be used
     * 
     * @throws NullPointerException
     *             if the scheduled executor service is <code>null</code>
     */
    public DelayedTaskEvictionScheduler(ScheduledExecutorService ses) {
        super();

        if (ses == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        this.ses = ses;
    }

    /**
     * Creates a delayed task eviction scheduler with the specified queue and a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @throws NullPointerException
     *             if the queue is <code>null</code>
     */
    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates a delayed task eviction scheduler with the specified queue and
     * scheduled executor service.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @param executorService
     *            the scheduled executor service to be used
     * 
     * @throws NullPointerException
     *             if either the queue or the scheduled executor service is
     *             <code>null</code>
     */
    public DelayedTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService executorService) {
        super(queue);
        if (executorService == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        this.ses = executorService;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>shutdownNow</tt> method on the
     * scheduled executor service.
     * </p>
     */
    @Override
    public void shutdown() {
        ses.shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the current next eviction time is different
     * from the last next eviction time, and if so (re)schedules the task.
     * </p>
     * 
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
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
     * This implementation checks if the current next eviction time is different
     * from the last next eviction time, and if so (re)schedules the task.
     * </p>
     * 
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
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
     * </p>
     * 
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
     */
    @Override
    protected void onEvictEntries() {
        schedule();
    }

    /**
     * (Re)schedules the task atomically. This method is synchronized to ensure
     * atomicity.
     */
    private synchronized void scheduleTask() {
        // Cancel the task if already scheduled, then call schedule() to
        // schedule a new task.
        if (future != null) {
            future.cancel(false);
        }

        schedule();
    }

    /**
     * Schedules the task atomically. This method is synchronized to ensure
     * atomicity. The next eviction time is always queried within this
     * synchronized method and not passed as parameter to ensure consistency in
     * a concurrent environment.
     */
    private synchronized void schedule() {
        // Get the next eviction time and reschedule the task with a delay
        // corresponding to the
        // difference between this time and the current time. If the next
        // eviction time is 0
        // (the queue is empty), don't schedule anything.
        next = getNextEvictionTime();
        future = (next > 0) ? ses.schedule(new EvictionRunnable(), Math.max(next - System.nanoTime(), 0), NANOSECONDS) : null;
    }

}
