package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class ConcurrentHashMapWithTimedEvictionTest {

    private static final String VALUE = "value";
    private static final int NUM_ITERATIONS = 20;
    private static final long TIMEOUT_MS = 60 * 60 * 1000;
    private static final int MAX_THREADS = 100;

    private static final int RESULT_PASSED = 0;
    private static final int RESULT_INTERRUPTED = -1;
    private static final int RESULT_ASSERTION_FAILED = -2;

    private static final int ES = 4;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] { 
            { ES, 1 }, { ES, 50 }, 
        });
        // @formatter:on
    }

    private static ScheduledThreadPoolExecutor evictionExecutor;
    private ThreadPoolExecutor testExecutor;

    private final long evictMs;
    private final int numThreads;

    private ConcurrentMapWithTimedEviction<Integer, String> map;

    public ConcurrentHashMapWithTimedEvictionTest(long evictMs, int numThreads) {
        super();
        this.evictMs = evictMs;
        this.numThreads = numThreads;
    }

    @BeforeClass
    public static void setUpClass() {
        evictionExecutor = new ScheduledThreadPoolExecutor(MAX_THREADS);
    }

    @AfterClass
    public static void tearDownClass() {
        evictionExecutor.shutdownNow();
    }

    @Before
    public void setUp() {
        map = new ConcurrentMapWithTimedEvictionDecorator<>(
            new ConcurrentHashMap<Integer, EvictibleEntry<Integer, String>>(), evictionExecutor);
        evictionExecutor.getQueue().clear();
        testExecutor = new ThreadPoolExecutor(numThreads, Integer.MAX_VALUE, 0, NANOSECONDS,
            new ArrayBlockingQueue<Runnable>(numThreads, true));
    }

    private interface TestTask {
        void test(int id) throws InterruptedException;
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(0, map.size());
        long t0 = System.nanoTime();
        assertNull(map.put(0, VALUE + 0, evictMs));
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
                assertNull(map.put(id, VALUE + id, evictMs));
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
                String value = VALUE + id;
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
                String value = VALUE + id;
                int id2 = id + 100;
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
                String value = VALUE + id;
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
                String value = VALUE + id;
                String value2 = VALUE + id + "x";
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
                String value = VALUE + id;
                String value2 = VALUE + id + "x";
                long t0 = System.nanoTime();
                assertNull(map.putIfAbsent(id, value, evictMs));
                assertQueueSize(1);
                assertTrue((evictionExecutor.getQueue().size() == 1) || numThreads > 1);
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
                String value = VALUE + id;
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
                String value = VALUE + id;
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
        run("testRemove2", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = VALUE + id;
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
    public void testClear() throws Exception {
        map.clear();
        assertTrue(map.isEmpty());
        long t0 = System.nanoTime();
        assertNull(map.put(0, VALUE + 0, evictMs));
        assertQueueSize(1);
        assertTrue(!map.isEmpty() || tooLate(t0));
        map.clear();
        assertAllDone();
        assertTrue(map.isEmpty());
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

    private void run(String name, TestTask task) throws InterruptedException {
//        System.out.println("[" + evictMs + ", " + numThreads + "] " + name);
        TestRunnable[] runnables = new TestRunnable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            runnables[i] = new TestRunnable(i, task);
            testExecutor.submit(runnables[i]);
        }
        testExecutor.shutdown();
        boolean terminated = testExecutor.awaitTermination(TIMEOUT_MS, MILLISECONDS);
        assertTrue(terminated);
        long sum = 0;
        for (TestRunnable r : runnables) {
            if (r.getError() != null) {
                throw r.getError();
            }
            assertTrue("Runnable " + r.getId() + " failed at iteration " + r.getIteration()
                + " with result " + r.getResult(), (r.getResult() == RESULT_PASSED));
            sum += r.getDurationNs();
        }
//        System.out.println("Time: " + ((double) (sum / numThreads) / 1000000.0) + " ms");
    }

    private final class TestRunnable implements Runnable {

        private final int id;
        private final TestTask task;
        private long durationNs = 0;
        private int result = RESULT_PASSED;
        private int iteration;
        private AssertionError error = null;

        public TestRunnable(int id, TestTask task) {
            super();
            this.id = id;
            this.task = task;
        }

        public int getId() {
            return id;
        }

        public long getDurationNs() {
            return durationNs;
        }

        public int getResult() {
            return result;
        }

        public int getIteration() {
            return iteration;
        }

        public AssertionError getError() {
            return error;
        }

        @Override
        public void run() {
//            System.out.println("Thread " + id + ": TestRunnable.run() start");
            long startNs = System.nanoTime();
            for (int i = 0; i < NUM_ITERATIONS; i++) {
                iteration = i;
                if (numThreads == 1) {
                    evictionExecutor.getQueue().clear();
                }
//                System.out.println("Thread " + id + ": TestRunnable.run() iteration " + iteration);
                try {
                    task.test(id * 1000 + iteration);
                } catch (InterruptedException e) {
                    result = RESULT_INTERRUPTED;
                    break;
                } catch (AssertionError e) {
                    result = RESULT_ASSERTION_FAILED;
                    error = e;
                    break;
                }
            }
            long endNs = System.nanoTime();
            durationNs = (endNs - startNs);
//            System.out.println("Thread " + id + ": TestRunnable.run() end");
        }
    }

}
