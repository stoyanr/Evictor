package com.stoyanr.evictor;

import java.lang.ref.WeakReference;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An {@link EvictionScheduler} which uses a {@link java.util.concurrent.ScheduledExecutorService}
 * to schedule multiple tasks for entries that should be evicted, one task per entry.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ExecutorServiceEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    public static final int DEFAULT_THREAD_POOL_SIZE = 1;

    private final ScheduledExecutorService ses;

    /**
     * Creates an eviction scheduler with a {@link java.util.concurrent.ScheduledThreadPoolExecutor}
     * .
     */
    public ExecutorServiceEvictionScheduler() {
        this(new ScheduledThreadPoolExecutor(DEFAULT_THREAD_POOL_SIZE));
    }

    /**
     * Creates an eviction scheduler with the specified scheduled executor service.
     * 
     * @param ses the scheduled executor service to be used
     * @throws NullPointerException if the scheduled executor service is null
     */
    public ExecutorServiceEvictionScheduler(ScheduledExecutorService ses) {
        super();
        if (ses == null)
            throw new NullPointerException();
        this.ses = ses;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            ScheduledFuture<?> future = ses.schedule(new EvictionRunnable<K, V>(e),
                Math.max(e.getEvictionTime() - System.nanoTime(), 0), TimeUnit.NANOSECONDS);
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
     * This implementation simply invokes the <tt>shutdownNow</tt> method on the scheduled executor
     * service.
     */
    @Override
    public void shutdown() {
        ses.shutdownNow();
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
