package com.stoyanr.evictor;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * An {@link EvictionQueue} which uses a {@link java.util.Queue} to store its entries. The queue
 * should support priority queue semantics and the comparator
 * {@link PriorityEvictionQueue.EvictibleEntryComparator} should always be used for comparing
 * entries.
 * 
 * <p>
 * The interface used is {@link java.util.Queue} and not {@link java.util.PriorityQueue} since the
 * most obvious concurrent implementation, {@link PriorityBlockingQueue} only conforms to the former
 * interface. This allows passing a different queue implementation, as far as it is concurrent
 * (thread-safe) and has priority semantics.
 * 
 * @author Stoyan Rachev
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class PriorityEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final Queue<EvictibleEntry<K, V>> queue;

    /**
     * Creates a priority eviction queue with a {@link java.util.concurrent.PriorityBlockingQueue}
     * with the specified initial capacity.
     * 
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if initial capacity is less than 1
     */
    public PriorityEvictionQueue(int initialCapacity) {
        this(new PriorityBlockingQueue<EvictibleEntry<K, V>>(initialCapacity,
            new EvictibleEntryComparator<K, V>()));
    }

    /**
     * Creates a priority eviction queue with the specified queue.
     * 
     * @param queue the queue to be used
     * @throws NullPointerException if the queue is null
     */
    public PriorityEvictionQueue(Queue<EvictibleEntry<K, V>> queue) {
        if (queue == null)
            throw new NullPointerException();
        this.queue = queue;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns true if the queue is non-empty and vice versa.
     */
    @Override
    public boolean hasEntries() {
        return !queue.isEmpty();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply returns the eviction time of the first entry in the queue if it is
     * non-empty, or 0 otherwise.
     */
    @Override
    public long getNextEvictionTime() {
        try {
            return (!queue.isEmpty()) ? queue.peek().getEvictionTime() : 0;
        } catch (NullPointerException e) {
            // Safeguard in a concurrent environment
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>add</tt> method on the queue.
     */
    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        queue.add(e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation simply invokes the <tt>remove</tt> method on the queue.
     */
    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        queue.remove(e);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * This implementation uses the <tt>peek</tt> and <tt>poll</tt> method on the queue repeatedly
     * to find and remove all entries that should be evicted, evicting them via their <tt>evict</tt>
     * method.
     */
    @Override
    public boolean evictEntries() {
        boolean result = false;
        try {
            while (!queue.isEmpty() && (queue.peek().getEvictionTime() < System.nanoTime())) {
                queue.poll().evict(false);
                result = true;
            }
        } catch (NullPointerException e) {
            // Safeguard in a concurrent environment
        }
        return result;
    }

    /**
     * A comparator that compares {@link EvictibleEntry} instances based on their eviction time.
     * 
     * @author Stoyan Rachev
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     */
    public static class EvictibleEntryComparator<K, V> implements Comparator<EvictibleEntry<K, V>> {

        @Override
        public int compare(EvictibleEntry<K, V> e1, EvictibleEntry<K, V> e2) {
            long t1 = e1.getEvictionTime(), t2 = e2.getEvictionTime();
            return (t1 > t2) ? 1 : (t1 < t2) ? -1 : 0;
        }
    }

}
