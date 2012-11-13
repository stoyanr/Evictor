package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ScheduledExecutorService;

import com.stoyanr.evictor.ConcurrentMapWithTimedEvictionDecorator;
import com.stoyanr.evictor.EvictibleEntry;
import com.stoyanr.evictor.SingleRegularTaskEvictionScheduler;

public class TestSingleRegularTaskEvictionScheduler<K, V> extends
    SingleRegularTaskEvictionScheduler<K, V> {

    public static final long EPSILON = 500000;

    private final boolean testInvariants;

    volatile int onScheduleEvictionCalls = 0;
    volatile int onCancelEvictionCalls = 0;
    volatile int onCancelAllEvictionCalls = 0;
    volatile int onEvictEntriesCalls = 0;

    public TestSingleRegularTaskEvictionScheduler(ScheduledExecutorService ses,
        boolean testInvariants) {
        super(ses);
        this.testInvariants = testInvariants;
    }

    @Override
    protected void onScheduleEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        synchronized (this) {
            onScheduleEvictionCalls++;
        }
        super.onScheduleEviction(map, e);
        testInvariants();
    }

    @Override
    protected void onCancelEviction(ConcurrentMapWithTimedEvictionDecorator<K, V> map,
        EvictibleEntry<K, V> e) {
        synchronized (this) {
            onCancelEvictionCalls++;
        }
        super.onCancelEviction(map, e);
        testInvariants();
    }

    @Override
    protected void onCancelAllEvictions(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        synchronized (this) {
            onCancelAllEvictionCalls++;
        }
        super.onCancelAllEvictions(map);
        testInvariants();
    }

    @Override
    protected void onEvictEntries(ConcurrentMapWithTimedEvictionDecorator<K, V> map) {
        synchronized (this) {
            onEvictEntriesCalls++;
        }
        super.onEvictEntries(map);
        testInvariants();
    }

    private void testInvariants() {
        if (testInvariants) {
            if (queue.isEmpty()) {
                assertNull(future);
            } else {
                assertTrue(future != null);
                assertFalse(future.isDone());
                assertTrue(isScheduledOk());
            }
        }
    }
    
    private boolean isScheduledOk() {
        long diff = now() + future.getDelay(NANOSECONDS) - queue.firstKey();
        boolean ok = (diff < EPSILON);
        if (!ok) {
            System.out.println(String.format("Diff: %d", diff));
        }
        return ok;
    }
}
