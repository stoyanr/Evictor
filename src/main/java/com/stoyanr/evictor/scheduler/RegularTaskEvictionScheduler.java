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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.stoyanr.evictor.EvictionQueue;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * A concrete implementation of {@link AbstractQueueEvictionScheduler} which
 * uses a single regular task scheduled in an
 * {@link java.util.concurrent.ScheduledExecutorService} to manage the automated
 * eviction. The task is scheduled with a fixed delay, independently of the time
 * of the next scheduled eviction. If the queue is empty, the task is cancelled
 * until an entry is added.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class RegularTaskEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    public static final long DEFAULT_DELAY = 1;

    public static final TimeUnit DEFAULT_TIME_UNIT = MILLISECONDS;

    private final ScheduledExecutorService executorService;

    private final long delay;

    private final TimeUnit timeUnit;

    private volatile ScheduledFuture<?> future = null;

    private volatile boolean active = false;

    /**
     * Creates a regular task eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}), a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}, and a default
     * delay of 1 millisecond.
     */
    public RegularTaskEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE), DEFAULT_DELAY, DEFAULT_TIME_UNIT);
    }

    /**
     * Creates a regular task eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}), a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}, and the
     * specified delay.
     * 
     * @param delay
     *            the delay between the termination of one execution and the
     *            commencement of the next
     * 
     * @param timeUnit
     *            the time unit of the delay parameter
     * 
     * @throws IllegalArgumentException
     *             if delay is less than or equal to zero
     */
    public RegularTaskEvictionScheduler(long delay, TimeUnit timeUnit) {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE), delay, timeUnit);
    }

    /**
     * Creates a regular task eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}) and the
     * specified scheduled executor service and delay.
     * 
     * @param executorService
     *            the scheduled executor service to be used
     * 
     * @param delay
     *            the delay between the termination of one execution and the
     *            commencement of the next
     * 
     * @param timeUnit
     *            the time unit of the delay parameter
     * 
     * @throws NullPointerException
     *             if the scheduled executor service is null
     * 
     * @throws IllegalArgumentException
     *             if delay is less than or equal to zero
     */
    public RegularTaskEvictionScheduler(ScheduledExecutorService executorService, long delay, TimeUnit timeUnit) {
        super();
        if (executorService == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        if (delay <= 0) {
            throw new IllegalArgumentException("Delay cannot be less than or equal to zero");
        }

        this.executorService = executorService;
        this.delay = delay;
        this.timeUnit = timeUnit;
    }

    /**
     * Creates a regular task eviction scheduler with the specified queue, a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}, and a default
     * delay of 1 millisecond.
     * 
     * @param queue
     *            the queue to be used
     */
    public RegularTaskEvictionScheduler(EvictionQueue<K, V> queue) {
        this(queue, new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE), DEFAULT_DELAY, DEFAULT_TIME_UNIT);
    }

    /**
     * Creates a regular task eviction scheduler with the specified queue,
     * scheduled executor service, and delay.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @param ses
     *            the scheduled executor service to be used
     * 
     * @param delay
     *            the delay between the termination of one execution and the
     *            commencement of the next
     * 
     * @param timeUnit
     *            the time unit of the delay parameter
     * 
     * @throws NullPointerException
     *             if either the queue or the scheduled executor service is null
     * 
     * @throws IllegalArgumentException
     *             if delay is less than or equal to zero
     */
    public RegularTaskEvictionScheduler(EvictionQueue<K, V> queue, ScheduledExecutorService ses, long delay, TimeUnit timeUnit) {
        super(queue);
        if (ses == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        if (delay <= 0) {
            throw new IllegalArgumentException("Delay cannot be less than or equal to zero");
        }

        this.executorService = ses;
        this.delay = delay;
        this.timeUnit = timeUnit;
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
        this.executorService.shutdownNow();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the executor has scheduled evictions and
     * the current state of the task is not active, and if so schedules the
     * regular task.
     * </p>
     * 
     * @throws RejectedExecutionException
     *             if the task cannot be scheduled for execution
     */
    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (hasScheduledEvictions() && !this.active) {
            schedule();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the executor does not have scheduled
     * evictions and the current state of the task is active, and if so cancels
     * the regular task.
     * </p>
     */
    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (!hasScheduledEvictions() && this.active) {
            cancel();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the executor does not have scheduled
     * evictions and the current state of the task is active, and if so cancels
     * the regular task.
     * </p>
     */
    @Override
    protected void onEvictEntries() {
        if (!hasScheduledEvictions() && this.active) {
            cancel();
        }
    }

    /**
     * Schedules the task atomically. This method is synchronized to ensure
     * atomicity. The active state is always queried within this synchronized
     * method and not passed as a parameter to ensure consistency in a
     * concurrent environment.
     */
    private synchronized void schedule() {
        // Check whether we have evictions scheduled and schedule the task if
        // not currently
        // active, but there are evictions.
        this.active = hasScheduledEvictions();
        if (this.future == null && this.active) {
            this.future = this.executorService.scheduleWithFixedDelay(new EvictionRunnable(), this.delay, this.delay, this.timeUnit);
        }
    }

    /**
     * Cancels the task atomically. This method is synchronized to ensure
     * atomicity. The active state is always queried within this synchronized
     * method and not passed as a parameter to ensure consistency in a
     * concurrent environment.
     */
    private synchronized void cancel() {
        // Check whether we have evictions scheduled and cancel the task if
        // currently active
        // but there are no evictions.
        this.active = hasScheduledEvictions();
        if (this.future != null && !this.active) {
            this.future.cancel(false);
            this.future = null;
        }
    }

}
