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
import com.stoyanr.evictor.EvictionScheduler;
import com.stoyanr.evictor.map.EvictibleEntry;
import com.stoyanr.evictor.queue.NavigableMapEvictionQueue;

/**
 * An abstract {@link EvictionScheduler} which uses an {@link EvictionQueue} to
 * store {@link EvictibleEntry} instances in the order they should be evicted.
 * This class does not implement the actual eviction functionality, it should be
 * implemented by its subclasses.
 * 
 * @author Stoyan Rachev
 * 
 * @param <K>
 *            the type of keys maintained by this map
 * 
 * @param <V>
 *            the type of mapped values
 */
public abstract class AbstractQueueEvictionScheduler<K, V> implements EvictionScheduler<K, V> {

    private final EvictionQueue<K, V> queue;

    /**
     * Creates an eviction scheduler with a {@link NavigableMapEvictionQueue}.
     */
    public AbstractQueueEvictionScheduler() {
        this(new NavigableMapEvictionQueue<K, V>());
    }

    /**
     * Creates an eviction scheduler with the specified queue.
     * 
     * @param queue
     *            the queue to be used
     * 
     * @throws NullPointerExceptin
     *             if the queue is <code>null</code>
     */
    public AbstractQueueEvictionScheduler(EvictionQueue<K, V> queue) {
        super();
        if (queue == null) {
            throw new NullPointerException("Queue cannot be null");
        }

        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scheduleEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.putEntry(e);
            onScheduleEviction(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cancelEviction(EvictibleEntry<K, V> e) {
        if (e.isEvictible()) {
            queue.removeEntry(e);
            onCancelEviction(e);
        }
    }

    /**
     * Evicts all entries by calling the <tt>evictEntries</tt> method on the
     * queue. This method should be called by tasks running in dedicated
     * eviction threads that manage the automated eviction.
     */
    protected void evictEntries() {
        if (queue.evictEntries()) {
            onEvictEntries();
        }
    }

    /**
     * Returns <code>true</code> if there are any evictions scheduled. This
     * simply calls the <tt>hasEntries</tt> method on the queue.
     */
    protected boolean hasScheduledEvictions() {
        return queue.hasEntries();
    }

    /**
     * Gets the time of the next scheduled eviction, in nanoseconds. This simply
     * calls the <tt>getNextEvictionTime</tt> method on the queue.
     */
    protected long getNextEvictionTime() {
        return queue.getNextEvictionTime();
    }

    /**
     * Actually schedules the eviction of the specified entry. It is guaranteed
     * that this entry is evictible. Subclasses should override this method to
     * provide the actual scheduling functionality.
     * 
     * @param e
     *            the entry for which the eviction should be scheduled
     */
    protected abstract void onScheduleEviction(EvictibleEntry<K, V> e);

    /**
     * Actually cancels the eviction of the specified entry. It is guaranteed
     * that this entry is evictible. Subclasses should override this method to
     * provide the actual cancellation functionality.
     * 
     * @param e
     *            the entry for which the eviction should be cancelled
     */
    protected abstract void onCancelEviction(EvictibleEntry<K, V> e);

    /**
     * Performs additional activities upon automated eviction of entries, if
     * needed. Subclasses should override this method if they need to perform
     * such activities.
     */
    protected abstract void onEvictEntries();

    final class EvictionRunnable implements Runnable {
        @Override
        public void run() {
            evictEntries();
        }
    }
}
