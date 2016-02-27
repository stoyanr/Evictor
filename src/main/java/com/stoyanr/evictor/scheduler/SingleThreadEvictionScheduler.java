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

import com.stoyanr.evictor.EvictionQueue;
import com.stoyanr.evictor.map.EvictibleEntry;

/**
 * A concrete implementation of {@link AbstractQueueEvictionScheduler} which
 * uses a single thread to manage the automated eviction. The thread waits until
 * the next scheduled eviction time before trying to perform an automated
 * eviction. It is notified appropriately each time an entry is added or removed
 * from the queue to ensure that it will always wake-up at the time of the next
 * scheduled eviction, not sooner or later. If the queue is empty, the thread is
 * waiting until notified when an entry is added. The behavior is similar to
 * that of {@link DelayedTaskEvictionScheduler}, but it is implemented at a
 * lower level, using a specially crafted thread rather than a scheduled
 * executor service.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public class SingleThreadEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    private volatile boolean finished = false;

    private volatile boolean notified = false;

    private volatile long next = 0;

    private final Thread evictionThread = new Thread(new EvictionThread());

    private final Object mutex = new Object();

    /**
     * Creates a single thread eviction scheduler with the default queue
     * implementation (see {@link AbstractQueueEvictionScheduler}). This
     * constructor starts the eviction thread, which will remain active until
     * the shutdown method is called.
     */
    public SingleThreadEvictionScheduler() {
        super();
        evictionThread.start();
    }

    /**
     * Creates a single thread eviction scheduler with the specified queue. This
     * constructor starts the eviction thread, which will remain active until
     * the shutdown method is called.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @throws NullPointerException
     *             if the queue is <code>null</code>
     */
    public SingleThreadEvictionScheduler(EvictionQueue<K, V> queue) {
        super(queue);
        evictionThread.start();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation causes the eviction thread to terminate.
     * </p>
     */
    @Override
    public void shutdown() {
        finished = true;
        evictionThread.interrupt();
        try {
            evictionThread.join();
        } catch (InterruptedException e) {
            // TODO: add this
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the current next eviction time is different
     * from the last next eviction time, and if so notifies the eviction thread,
     * causing it to wake-up and recalculate its waiting time.
     * </p>
     */
    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            synchronized (mutex) {
                notified = true;
                mutex.notifyAll();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation checks if the current next eviction time is different
     * from the last next eviction time, and if so notifies the eviction thread,
     * causing it to wake-up and recalculate its waiting time.
     * </p>
     */
    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            synchronized (mutex) {
                notified = true;
                mutex.notifyAll();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation does nothing.
     * </p>
     */
    @Override
    protected void onEvictEntries() {
        // Do nothing
    }

    /*
     * Eviction thread runnable.
     */
    final class EvictionThread implements Runnable {

        /**
         * Runs the eviction thread.
         */
        @Override
        public void run() {
            while (!finished) {
                next = getNextEvictionTime();
                long timeout = calcTimeout(next);
                while (timeout >= 0) {
                    // The timeout is 0 (forever) or positive - wait
                    if (!waitFor(timeout) && !finished) {
                        // The timeout did not expire and we are not finished -
                        // calculate the next timeout
                        next = getNextEvictionTime();
                        timeout = calcTimeout(next);
                    } else {
                        // The timeout expired or we are finished - get out
                        break;
                    }
                }

                evictEntries();
            }
        }

        /**
         * Calculates the wait timeout (in nanoseconds) to the specified moment
         * in time (in nanoseconds). If the time is 0 (forever), return also 0
         * (forever). A negative value returned from this method means that no
         * waiting should happen.
         */
        private long calcTimeout(long time) {
            if (time > 0) {
                long x = time - System.nanoTime();
                return (x != 0) ? x : -1;
            }

            return 0;
        }

        /**
         * Waits for the specified timeout (in nanoseconds). Returns true if the
         * timeout expired and false if thread was notified or interrupted.
         */
        private boolean waitFor(long timeout) {
            boolean result = true;
            try {
                synchronized (mutex) {
                    notified = false;
                    mutex.wait(timeout / 1000000, (int) (timeout % 1000000));
                    result = !notified;
                }
            } catch (InterruptedException e) {
                result = false;
            }

            return result;
        }
    }

}
