package com.stoyanr.evictor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import com.stoyanr.evictor.ConcurrentHashMapWithTimedEviction;
import com.stoyanr.evictor.EvictionScheduler;
import com.stoyanr.evictor.MultiTaskEvictionScheduler;
import com.stoyanr.evictor.NullEvictionScheduler;
import com.stoyanr.evictor.SingleDelayedTaskEvictionScheduler;
import com.stoyanr.evictor.SingleRegularTaskEvictionScheduler;

public abstract class AbstractConcurrentMapWithTimedEvictionTest {

    public static final String VALUE = "value";
    public static final String VALUE2 = "valuex";
    public static final long TIMEOUT_MS = 60 * 60 * 1000;
    public static final int MAX_EVICTION_THREADS = 1;
    public static final int MAX_MAP_SIZE = 1000000;
    public static final float LOAD_FACTOR = 0.75f;

    public static final int RESULT_PASSED = 0;
    public static final int RESULT_INTERRUPTED = -1;
    public static final int RESULT_ASSERTION_FAILED = -2;

    public static final int IMPL_CHM = 0;
    public static final int IMPL_CHMWTE_NULL = 1;
    public static final int IMPL_CHMWTE_MULTI_TASK = 2;
    public static final int IMPL_CHMWTE_SINGLE_REG_TASK = 3;
    public static final int IMPL_CHMWTE_SINGLE_DEL_TASK = 4;
    public static final int IMPL_CHMWTE_SINGLE_REG_TASK_T = 5;
    public static final int IMPL_CHMWTE_SINGLE_DEL_TASK_T = 6;

    protected final int impl;
    protected final long evictMs;
    protected final int numThreads;
    protected final int numIterations;

    protected ScheduledThreadPoolExecutor evictionExecutor;
    protected ThreadPoolExecutor testExecutor;
    protected ConcurrentMap<Integer, String> map;
    protected EvictionScheduler<Integer, String> scheduler;

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
        createScheduler();
        createMap();
    }

    public void tearDown() {
        evictionExecutor.shutdownNow();
    }

    public abstract void setUpIteration();

    public abstract void tearDownIteration();

    private void createScheduler() {
        switch (impl) {
        case IMPL_CHMWTE_NULL:
            scheduler = new NullEvictionScheduler<>();
            break;
        case IMPL_CHMWTE_MULTI_TASK:
            scheduler = new MultiTaskEvictionScheduler<>(evictionExecutor);
            break;
        case IMPL_CHMWTE_SINGLE_REG_TASK:
            scheduler = new SingleRegularTaskEvictionScheduler<>(evictionExecutor);
            break;
        case IMPL_CHMWTE_SINGLE_DEL_TASK:
            scheduler = new SingleDelayedTaskEvictionScheduler<>(evictionExecutor);
            break;
        case IMPL_CHMWTE_SINGLE_REG_TASK_T:
            scheduler = new TestSingleRegularTaskEvictionScheduler<>(evictionExecutor,
                (numThreads == 1));
            break;
        case IMPL_CHMWTE_SINGLE_DEL_TASK_T:
            scheduler = new TestSingleDelayedTaskEvictionScheduler<>(evictionExecutor,
                (numThreads == 1));
            break;
        }
    }

    private void createMap() {
        int capacity = Math.min(numThreads * numIterations, MAX_MAP_SIZE);
        switch (impl) {
        case IMPL_CHM:
            map = new ConcurrentHashMap<>(capacity, LOAD_FACTOR, numThreads);
            break;
        case IMPL_CHMWTE_NULL:
        case IMPL_CHMWTE_MULTI_TASK:
        case IMPL_CHMWTE_SINGLE_REG_TASK:
        case IMPL_CHMWTE_SINGLE_DEL_TASK:
        case IMPL_CHMWTE_SINGLE_REG_TASK_T:
        case IMPL_CHMWTE_SINGLE_DEL_TASK_T:
            map = new ConcurrentHashMapWithTimedEviction<>(capacity, LOAD_FACTOR, numThreads,
                scheduler);
            break;
        }
    }

    protected interface TestTask {
        public void test(int id) throws InterruptedException;
    }

    protected void run(String name, TestTask task) throws InterruptedException {
        printHeader(name);
        TestRunnable[] runnables = createRunnables(task);
        executeRunnables(runnables);
        checkResults(runnables);
        printAverageTime(calcAverageTime(runnables));
        printStatistics();
    }

    private TestRunnable[] createRunnables(TestTask task) {
        TestRunnable[] runnables = new TestRunnable[numThreads];
        for (int i = 0; i < numThreads; i++) {
            runnables[i] = new TestRunnable(i, task);
        }
        return runnables;
    }

    private void executeRunnables(TestRunnable[] runnables) throws InterruptedException {
        for (int i = 0; i < numThreads; i++) {
            testExecutor.submit(runnables[i]);
        }
        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(TIMEOUT_MS, MILLISECONDS));
    }

    private void checkResults(TestRunnable[] runnables) throws AssertionError {
        for (TestRunnable r : runnables) {
            if (r.getError() != null) {
                throw r.getError();
            }
            assertTrue(r.getResult() == RESULT_PASSED);
        }
    }

    private double calcAverageTime(TestRunnable[] runnables) {
        long sum = 0;
        for (TestRunnable r : runnables) {
            sum += r.getDurationNs();
        }
        return ((double) sum) / (numThreads * numIterations * 1000.0);
    }

    private void printHeader(String name) {
        // @formatter:off
        System.out.printf("%s(%s), %d ms, %d threads, %d iterations: %s\n", 
            map.getClass().getSimpleName(), 
            ((scheduler != null) ? scheduler.getClass().getSimpleName() : ""),
            evictMs, numThreads, numIterations, name);
        // @formatter:on
    }

    private void printAverageTime(double avgTime) {
        System.out.printf("Average time: %f us\n", avgTime);
    }

    private void printStatistics() {
        if (scheduler instanceof TestSingleDelayedTaskEvictionScheduler) {
            printStatistics((TestSingleDelayedTaskEvictionScheduler<Integer, String>) scheduler);
        } else if (scheduler instanceof TestSingleRegularTaskEvictionScheduler) {
            printStatistics((TestSingleRegularTaskEvictionScheduler<Integer, String>) scheduler);
        }
    }

    private void printStatistics(TestSingleRegularTaskEvictionScheduler<Integer, String> ts) {
        // @formatter:off
        System.out.printf("Scheduler: onScheduleEviction: %d, onCancelEviction: %d, onCancelAllEvictions: %d, onEvictEntries: %d\n", 
            ts.onScheduleEvictionCalls, ts.onCancelEvictionCalls, ts.onCancelAllEvictionCalls, ts.onEvictEntriesCalls);
        // @formatter:on
    }

    private void printStatistics(TestSingleDelayedTaskEvictionScheduler<Integer, String> ts) {
        // @formatter:off
        System.out.printf("Scheduler: onScheduleEviction: %d, onCancelEviction: %d, onCancelAllEvictions: %d, onEvictEntries: %d\n", 
            ts.onScheduleEvictionCalls, ts.onCancelEvictionCalls, ts.onCancelAllEvictionCalls, ts.onEvictEntriesCalls);
        // @formatter:on
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
                setUpIteration();
                int idx = getIterationId(id, i);
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
                    System.out.printf("Assertion failed: %d\n", idx);
                    break;
                }
                tearDownIteration();
            }
        }
    }

    protected int getIterationId(int id, int i) {
        return (id * numIterations + i) % MAX_MAP_SIZE;
    }

    protected static String getValue(int id) {
        return VALUE + id;
    }

    protected static String getValue2(int id) {
        return VALUE2 + id;
    }

    protected static int getId2(int id) {
        return Integer.MAX_VALUE - id;
    }

}
