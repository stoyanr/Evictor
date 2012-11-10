package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ConcurrentMapWithTimedEvictionTest extends AbstractConcurrentMapWithTimedEvictionTest {

    private static final int NUM_ITERATIONS = 10;
    private static final int EVICT_MS = 5;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] { 
            { IMPL_CHMWTE_MULTI_TASK, 1 }, { IMPL_CHMWTE_MULTI_TASK, 50 }, 
            { IMPL_CHMWTE_SINGLE_REG_TASK, 1 }, { IMPL_CHMWTE_SINGLE_REG_TASK, 50 },  
            { IMPL_CHMWTE_SINGLE_DEL_TASK, 1 }, { IMPL_CHMWTE_SINGLE_DEL_TASK, 50 },  
        });
        // @formatter:on
    }

    private ConcurrentMapWithTimedEviction<Integer, String> map;

    public ConcurrentMapWithTimedEvictionTest(int impl, int numThreads) {
        super(impl, EVICT_MS, numThreads, NUM_ITERATIONS);
    }

    @Before
    @Override
    public void setUp() {
        super.setUp();
        map = (ConcurrentMapWithTimedEviction<Integer, String>) super.map;
    }

    @After
    @Override
    public void tearDown() {
        super.tearDown();
    }

    @Override
    public void setUpIteration() {
        clearEvictionExecutorQueue();
    }

    @Override
    public void tearDownIteration() {
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(0, map.size());
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertTrue((map.size() == 1) || tooLate(t0));
        Thread.sleep(evictMs + 1);
        assertNull(map.get(0));
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsKey() throws Exception {
        run("testContainsKey", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                assertFalse(map.containsKey(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, getValue(id), evictMs));
                assertTrue(map.containsKey(id) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertFalse(map.containsKey(id));
            }
        });
    }

    @Test
    public void testContainsValue() throws Exception {
        run("testContainsValue", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertFalse(map.containsValue(value));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertTrue(map.containsValue(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertFalse(map.containsValue(value));
            }
        });
    }

    @Test
    public void testContainsValue2() throws Exception {
        run("testContainsValue2", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                int id2 = getId2(id);
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertTrue(map.containsValue(value) || tooLate(t0));
                long t1 = System.nanoTime();
                assertNull(map.put(id2, value, evictMs * 2));
                Thread.sleep(evictMs + 1);
                assertTrue(map.containsValue(value) || tooLate(t1, 2));
                assertTrue(map.containsKey(id2) || tooLate(t1, 2));
                assertFalse(map.containsKey(id));
                Thread.sleep(evictMs * 2 + 1);
                assertFalse(map.containsValue(value));
                assertFalse(map.containsKey(id2));
            }
        });
    }

    @Test
    public void testGet() throws Exception {
        run("testGet", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.get(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertTrue(map.get(id).equals(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testPut() throws Exception {
        run("testPut", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                long t1 = System.nanoTime();
                assertTrue(map.put(id, value2, evictMs).equals(value) || tooLate(t0));
                assertTrue(map.get(id).equals(value2) || tooLate(t1));
                Thread.sleep(evictMs + 1);
                assertNull(map.put(id, value));
                assertAllDone();
                assertEquals(value, map.get(id));
            }
        });
    }

    @Test
    public void testPutIfAbsent() throws Exception {
        run("testPutIfAbsent", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                long t0 = System.nanoTime();
                assertNull(map.putIfAbsent(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.putIfAbsent(id, value2, evictMs).equals(value) || tooLate(t0));
                assertTrue(map.get(id).equals(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertNull(map.putIfAbsent(id, value));
                assertAllDone();
                assertEquals(value, map.get(id));
            }
        });
    }

    @Test
    public void testRemove() throws Exception {
        run("testRemove", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.remove(id));
                assertNull(map.put(id, value));
                assertEquals(value, map.remove(id));
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testRemove2() throws Exception {
        run("testRemove2", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.remove(id));
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertNull(map.remove(id));
                assertAllDone();
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testRemove3() throws Exception {
        run("testRemove3", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.remove(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.remove(id).equals(value) || tooLate(t0));
                assertAllDone();
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testRemove4() throws Exception {
        run("testRemove4", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.remove(id, value));
                assertNull(map.put(id, value));
                assertFalse(map.remove(id, value2));
                assertTrue(map.remove(id, value));
                assertAllDone();
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testRemove5() throws Exception {
        run("testRemove5", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.remove(id, value));
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertFalse(map.remove(id, value2));
                assertAllDone();
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testClear() throws Exception {
        map.clear();
        assertTrue(map.isEmpty());
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertQueueSize(1);
        assertTrue(!map.isEmpty() || tooLate(t0));
        map.clear();
        assertAllDone();
        assertTrue(map.isEmpty());
    }

    private void clearEvictionExecutorQueue() {
        if (numThreads == 1) {
            evictionExecutor.getQueue().clear();
        }
    }

    private boolean tooLate(long t) {
        return tooLate(t, 1);
    }

    private boolean tooLate(long t, long factor) {
        long elapsed = System.nanoTime() - t;
        System.out.println("Too late: " + ((double) elapsed / 1000000.0) + " ms");
        return (elapsed > NANOSECONDS.convert(evictMs * factor, MILLISECONDS));
    }

    private void assertQueueSize(int expected) {
        if (numThreads == 1) {
            assertEquals(expected, evictionExecutor.getQueue().size());
        }
    }

    private void assertAllDone() {
        if (numThreads == 1) {
            for (Runnable runnable : evictionExecutor.getQueue()) {
                assertTrue(((ScheduledFuture<?>) runnable).isDone());
            }
        }
    }
    
}
