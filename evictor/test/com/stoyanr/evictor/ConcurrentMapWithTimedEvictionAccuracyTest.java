package com.stoyanr.evictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ConcurrentMapWithTimedEvictionAccuracyTest extends
    AbstractConcurrentMapWithTimedEvictionTest {

    private static final int NUM_THREADS = 5;
    private static final int NUM_ITERATIONS = 3;
    private static final int EVICT_MS = 5 * 1000;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] { 
            { IMPL_CHMWTE_ESS }, 
            { IMPL_CHMWTE_NM_RT },
            { IMPL_CHMWTE_NM_DT },
            { IMPL_CHMWTE_NM_ST },
            { IMPL_CHMWTE_PQ_ST },
        });
        // @formatter:on
    }

    private TestConcurrentMapWithTimedEvictionDecorator<Integer, String> mapx;

    public ConcurrentMapWithTimedEvictionAccuracyTest(int impl) {
        super(impl, EVICT_MS, NUM_THREADS, NUM_ITERATIONS);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mapx = (TestConcurrentMapWithTimedEvictionDecorator<Integer, String>) super.map;
        assertTrue(mapx.isEmpty());
    }

    @After
    @Override
    public void tearDown() throws Exception {
        System.out.printf("Sleeping for %d ms\n", evictMs);
        Thread.sleep(evictMs);
        assertEquals(0, mapx.size());
        System.out.printf("Min: %.2f us, Max: %.2f us, Average: %.2f us\n", mapx.min, mapx.max,
            mapx.sum / (numThreads * numIterations));
        super.tearDown();
    }

    @Override
    public void setUpIteration() {
    }

    @Override
    public void tearDownIteration() {
    }

    @Override
    protected void createMap() {
        int capacity = Math.min(numThreads * numIterations, MAX_MAP_SIZE);
        switch (impl) {
        case IMPL_CHMWTE_ESS:
        case IMPL_CHMWTE_NM_RT:
        case IMPL_CHMWTE_NM_DT:
        case IMPL_CHMWTE_NM_ST:
        case IMPL_CHMWTE_PQ_ST:
            map = new TestConcurrentMapWithTimedEvictionDecorator<Integer, String>(
                new ConcurrentHashMap<Integer, EvictibleEntry<Integer, String>>(capacity,
                    LOAD_FACTOR, numThreads), scheduler);
            break;
        }
    }

    @Test
    public void test() throws Exception {
        run("test", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                long sleepMs = getRandomEvictMs();
                Thread.sleep(sleepMs);
                String value = getValue(id);
                long evictMs;
                do {
                    evictMs = getRandomEvictMs();
                } while (evictMs == 0);
                System.out.printf("Putting [%d, %s, %d]\n", id, value, evictMs);
                mapx.put(id, value, evictMs);
            }
        });
    }

    private long getRandomEvictMs() {
        return (long) (Math.random() * evictMs);
    }

    private class TestConcurrentMapWithTimedEvictionDecorator<K, V> extends
        ConcurrentMapWithTimedEvictionDecorator<K, V> {

        private double max = Double.MIN_VALUE, min = Double.MAX_VALUE, sum = 0;

        public TestConcurrentMapWithTimedEvictionDecorator(
            ConcurrentMap<K, EvictibleEntry<K, V>> delegate, EvictionScheduler<K, V> scheduler) {
            super(delegate, scheduler);
        }

        @Override
        void evict(EvictibleEntry<K, V> e, boolean cancelPendingEviction) {
            double x = ((double) (System.nanoTime() - e.getEvictionTime())) / 1000.0;
            System.out.printf("Evicting %s after %.2f us\n", e.toString(), x);
            max = Math.max(max, x);
            min = Math.min(min, x);
            sum += x;
            super.evict(e, cancelPendingEviction);
        }
    }
}
