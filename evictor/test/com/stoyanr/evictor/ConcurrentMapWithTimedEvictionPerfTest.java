package com.stoyanr.evictor;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;

@RunWith(value = Parameterized.class)
public class ConcurrentMapWithTimedEvictionPerfTest extends
    AbstractConcurrentMapWithTimedEvictionTest {

    private static final int NUM_THREADS = 100;
    private static final int NUM_ITERATIONS = 20000;
    private static final int EVICT_MS = 100 * 1000;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] { 
            { IMPL_CHM }, 
            { IMPL_CHMWTE_NULL }, 
            { IMPL_CHMWTE_MULTI_TASK }, 
            { IMPL_CHMWTE_SINGLE_REG_TASK },
            { IMPL_CHMWTE_SINGLE_DEL_TASK }
        });
        // @formatter:on
    }

    public ConcurrentMapWithTimedEvictionPerfTest(int impl) {
        super(impl, EVICT_MS, NUM_THREADS, NUM_ITERATIONS);
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Override
    public void setUpIteration() {
    }

    @Override
    public void tearDownIteration() {
    }

    @Test
    public void testGet() throws Exception {
        populateMap(map);
        run("testGet", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                map.get(id);
            }
        });
    }

    @Test
    public void testGetWithEviction() throws Exception {
        if (!(map instanceof ConcurrentMapWithTimedEviction))
            return;
        final ConcurrentMapWithTimedEviction<Integer, String> mapx = (ConcurrentMapWithTimedEviction<Integer, String>) map;
        populateMapWithEviction(mapx);
        run("testGetWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                mapx.get(id);
            }
        });
    }

    @Test
    public void testPut() throws Exception {
        run("testPut", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                map.put(id, getValue(id));
            }
        });
    }

    @Test
    public void testPutWithEviction() throws Exception {
        if (!(map instanceof ConcurrentMapWithTimedEviction))
            return;
        final ConcurrentMapWithTimedEviction<Integer, String> mapx = (ConcurrentMapWithTimedEviction<Integer, String>) map;
        run("testPutWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                mapx.put(id, getValue(id), getRandomEvictMs());
            }
        });
    }

    private void populateMap(ConcurrentMap<Integer, String> map) {
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < numIterations; j++) {
                int id = getIterationId(i, j);
                map.put(id, getValue(id));
            }
        }
    }

    private void populateMapWithEviction(ConcurrentMapWithTimedEviction<Integer, String> map) {
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < numIterations; j++) {
                int id = getIterationId(i, j);
                map.put(id, getValue(id), getRandomEvictMs());
            }
        }
    }

    private long getRandomEvictMs() {
        return (long) (Math.random() * evictMs);
    }

}
