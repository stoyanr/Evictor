package com.stoyanr.evictor;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class PriorityEvictionQueue<K, V> implements EvictionQueue<K, V> {

    private final Queue<EvictibleEntry<K, V>> queue;

    public PriorityEvictionQueue(int initialCapacity) {
        this(new PriorityBlockingQueue<EvictibleEntry<K, V>>(initialCapacity,
            new EvictibleEntryComparator<K, V>()));
    }

    public PriorityEvictionQueue(Queue<EvictibleEntry<K, V>> queue) {
        assert (queue != null);
        this.queue = queue;
    }

    @Override
    public boolean hasEntries() {
        return !queue.isEmpty();
    }

    @Override
    public long getNextEvictionTime() {
        try {
            return (!queue.isEmpty()) ? queue.peek().getEvictionTime() : 0;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    @Override
    public void putEntry(EvictibleEntry<K, V> e) {
        queue.add(e);
    }

    @Override
    public void removeEntry(EvictibleEntry<K, V> e) {
        queue.remove(e);
    }

    @Override
    public boolean evictEntries() {
        boolean result = false;
        try {
            while (!queue.isEmpty() && (queue.peek().getEvictionTime() < System.nanoTime())) {
                EvictibleEntry<K, V> e = queue.poll();
                e.evict(false);
                result = true;
            }
        } catch (NullPointerException e) {
        }
        return result;
    }

    public static class EvictibleEntryComparator<K, V> implements Comparator<EvictibleEntry<K, V>> {

        @Override
        public int compare(EvictibleEntry<K, V> e1, EvictibleEntry<K, V> e2) {
            long t1 = e1.getEvictionTime(), t2 = e2.getEvictionTime();
            return (t1 > t2) ? 1 : (t1 < t2) ? -1 : 0;
        }
    }

}
