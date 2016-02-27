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

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.stoyanr.evictor.EvictionScheduler;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * An {@link EvictionScheduler} which uses a
 * {@link java.util.concurrent.ScheduledExecutorService} to schedule multiple
 * tasks for entries that should be evicted, one task per entry.
 * 
 * @author Stoyan Rachev
 *
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class ExecutorServiceEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService executorService;

    /**
     * Creates an eviction scheduler with a
     * {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
     */
    public ExecutorServiceEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates an eviction scheduler with the specified scheduled executor
     * service.
     * 
     * @param executorService
     *            the scheduled executor service to be used
     * 
     * @throws NullPointerException
     *             if the scheduled executor service is <code>null</code>
     */
    public ExecutorServiceEvictionScheduler(ScheduledExecutorService executorService) {
        super();
        if (executorService == null) {
            throw new NullPointerException("ScheduledExecutorService instance cannot be null");
        }

        this.executorService = executorService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            ScheduledFuture<?> future = executorService.schedule(new EvictionRunnable<K, V>(e), Math.max(e.getEvictionTime() - System.nanoTime(), 0), TimeUnit.NANOSECONDS);
            e.setData(future);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        ScheduledFuture<?> future = (ScheduledFuture<?>) e.getData();
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
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

    private static final class EvictionRunnable<K, V> implements Runnable {

        private final WeakReference<EvictibleEntry<K, V>> er;

        public EvictionRunnable(EvictibleEntry<K, V> e) {
            er = new WeakReference<EvictibleEntry<K, V>>(e);
        }

        @Override
        public void run() {
            EvictibleEntry<K, V> e = er.get();
            if (e != null) {
                e.evict(false);
            }
        }
    }

}
