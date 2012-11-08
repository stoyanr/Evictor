package com.stoyanr.concurrent;

import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

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
            { 0 }, { 1 }, { 2 }, { 3 }
        });
        // @formatter:on
    }

    public ConcurrentMapWithTimedEvictionPerfTest(int impl) {
        super(impl, EVICT_MS, NUM_THREADS, NUM_ITERATIONS);
    }

    @Before
    public void setUp() {
        super.setUp();
    }

    @After
    public void tearDown() {
        super.tearDown();
    }

    @Test
    public void testGet() throws Exception {
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                int id = i * 1000 + j;
                map.put(id, VALUE + id);
            }
        }
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
        for (int i = 0; i < numThreads; i++) {
            for (int j = 0; j < NUM_ITERATIONS; j++) {
                int id = i * 1000 + j;
                mapx.put(id, VALUE + id, (long) (Math.random() * evictMs));
            }
        }
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
                map.put(id, VALUE + id);
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
                mapx.put(id, VALUE + id, (long) (Math.random() * evictMs));
            }
        });
    }

}
