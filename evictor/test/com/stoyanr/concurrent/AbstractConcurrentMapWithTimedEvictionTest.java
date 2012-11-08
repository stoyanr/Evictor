package com.stoyanr.concurrent;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;

public class AbstractConcurrentMapWithTimedEvictionTest {

    public static final String VALUE = "value";
    public static final long TIMEOUT_MS = 60 * 60 * 1000;
    public static final int MAX_EVICTION_THREADS = 1;

    public static final int RESULT_PASSED = 0;
    public static final int RESULT_INTERRUPTED = -1;
    public static final int RESULT_ASSERTION_FAILED = -2;

    protected ScheduledThreadPoolExecutor evictionExecutor;
    protected ThreadPoolExecutor testExecutor;

    protected final int impl;
    protected final long evictMs;
    protected final int numThreads;
    protected final int numIterations;

    protected ConcurrentMap<Integer, String> map;

    public AbstractConcurrentMapWithTimedEvictionTest(int impl, long evictMs, int numThreads,
        int numIterations) {
        super();
        this.impl = impl;
        this.evictMs = evictMs;
        this.numThreads = numThreads;
        this.numIterations = numIterations;
    }

    public void setUp() {
        evictionExecutor = new ScheduledThreadPoolExecutor(MAX_EVICTION_THREADS);
        testExecutor = new ThreadPoolExecutor(numThreads, Integer.MAX_VALUE, 0, NANOSECONDS,
            new ArrayBlockingQueue<Runnable>(numThreads, true));
        createMap();
    }

    public void tearDown() {
        evictionExecutor.getQueue().clear();
    }

    protected void createMap() {
        switch (impl) {
        case 0:
            map = new ConcurrentHashMap<>();
            break;
        case 1:
            map = new ConcurrentHashMapWithTimedEviction<>(
                new MultiTaskEvictionScheduler<Integer, String>(evictionExecutor));
            break;
        case 2:
            map = new ConcurrentHashMapWithTimedEviction<>(
                new SingleTaskEvictionScheduler<Integer, String>(evictionExecutor));
            break;
        case 3:
            long capacity = numThreads * numIterations;
            map = new ConcurrentMapWithTimedEvictionDecorator<Integer, String>(
                new ConcurrentLinkedHashMap.Builder<Integer, EvictibleEntry<Integer, String>>()
                    .maximumWeightedCapacity(capacity).build(),
                new SingleTaskEvictionScheduler<Integer, String>(evictionExecutor));
            break;
        }
    }

    protected interface TestTask {
        void test(int id) throws InterruptedException;
    }

    protected void run(String name, TestTask task) throws InterruptedException {
        System.out.println("[" + evictMs + " ms, " + numThreads + " threads] " + name);
        System.out.println("Cache operations: " + numThreads * numIterations);
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
            assertTrue(r.getResult() == RESULT_PASSED);
            sum += r.getDurationNs();
        }
        System.out.println("Average time: "
            + ((double) sum / (numThreads * numIterations * 1000.0)) + " us");
    }

    private void clearEvictionExecutorQueue() {
        if (numThreads == 1) {
            evictionExecutor.getQueue().clear();
        }
    }

    private final class TestRunnable implements Runnable {

        private final int id;
        private final TestTask task;
        private long durationNs = 0;
        private int result = RESULT_PASSED;
        private AssertionError error = null;

        public TestRunnable(int id, TestTask task) {
            super();
            this.id = id;
            this.task = task;
        }

        public long getDurationNs() {
            return durationNs;
        }

        public int getResult() {
            return result;
        }

        public AssertionError getError() {
            return error;
        }

        @Override
        public void run() {
            for (int i = 0; i < numIterations; i++) {
                clearEvictionExecutorQueue();
                int idx = id * 1000 + i;
                try {
                    long startNs = System.nanoTime();
                    task.test(idx);
                    long endNs = System.nanoTime();
                    durationNs += (endNs - startNs);
                } catch (InterruptedException e) {
                    result = RESULT_INTERRUPTED;
                    break;
                } catch (AssertionError e) {
                    result = RESULT_ASSERTION_FAILED;
                    error = e;
                    System.out.println("Assertion failed: " + idx);
                    break;
                }
            }
        }
    }
}
