/**
 * 
 * Copyright 2012, Stoyan Rachev
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.stoyanr.evictor.map;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
    private static final int EVICT_MS = 8;
    private static final int DELAY_MS = 20;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] { 
            { IMPL_CHMWTE_ESS, 1 }, { IMPL_CHMWTE_ESS, 50 }, 
            { IMPL_CHMWTE_NM_RT, 1 }, { IMPL_CHMWTE_NM_RT, 50 },  
            { IMPL_CHMWTE_NM_DT, 1 }, { IMPL_CHMWTE_NM_DT, 50 },  
            { IMPL_CHMWTE_NM_ST, 1 }, { IMPL_CHMWTE_NM_ST, 50 },  
            { IMPL_CHMWTE_PQ_ST, 1 }, { IMPL_CHMWTE_PQ_ST, 50 },  
        });
        // @formatter:on
    }

    private ConcurrentMapWithTimedEvictionDecorator<Integer, String> map;

    public ConcurrentMapWithTimedEvictionTest(int impl, int numThreads) {
        super(impl, EVICT_MS, numThreads, NUM_ITERATIONS);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        map = (ConcurrentMapWithTimedEvictionDecorator<Integer, String>) super.map;
    }

    @After
    @Override
    public void tearDown() throws Exception {
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
        assertNull(map.put(0, getValue(0)));
        assertEquals(1, map.size());
    }

    @Test
    public void testSizeWithEviction() throws Exception {
        assertEquals(0, map.size());
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertQueueSize(1);
        assertTrue((map.size() == 1) || ((map.size() == 0) && tooLate(t0)));
        Thread.sleep(evictMs + DELAY_MS);
        assertEquals(0, map.size());
        assertAllDone();
    }

    @Test
    public void testContainsKey() throws Exception {
        run("testContainsKey", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                assertFalse(map.containsKey(id));
                assertNull(map.put(id, getValue(id)));
                assertTrue(map.containsKey(id));
            }
        });
    }

    @Test
    public void testContainsKeyWithEviction() throws Exception {
        run("testContainsKeyWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                assertFalse(map.containsKey(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, getValue(id), evictMs));
                assertQueueSize(1);
                assertTrue(map.containsKey(id) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertFalse(map.containsKey(id));
                assertAllDone();
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
                assertNull(map.put(id, value));
                assertTrue(map.containsValue(value));
            }
        });
    }

    @Test
    public void testContainsValueWithEviction() throws Exception {
        run("testContainsValueWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertFalse(map.containsValue(value));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.containsValue(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertFalse(map.containsValue(value));
                assertAllDone();
            }
        });
    }

    @Test
    public void testContainsValueWithEviction2() throws Exception {
        run("testContainsValueWithEviction2", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                int id2 = getId2(id);
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.containsValue(value) || tooLate(t0));
                long t1 = System.nanoTime();
                assertNull(map.put(id2, value, evictMs * 2));
                Thread.sleep(evictMs + 1);
                assertTrue(map.containsValue(value) || tooLate(t1, 2));
                Thread.sleep(evictMs * 2 + 1);
                assertFalse(map.containsValue(value));
                assertAllDone();
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
                assertNull(map.put(id, value));
                assertEquals(value, map.get(id));
            }
        });
    }

    @Test
    public void testGetWithEviction() throws Exception {
        run("testGetWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.get(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.get(id).equals(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertNull(map.get(id));
                assertAllDone();
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
                assertNull(map.put(id, value));
                assertEquals(value, map.put(id, value2));
                assertEquals(value2, map.get(id));
            }
        });
    }

    @Test
    public void testPutJustEvicted() throws Exception {
        run("testPutJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertNull(map.put(id, value2));
                assertEquals(value2, map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testPutToBeEvicted() throws Exception {
        run("testPutToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.put(id, value2).equals(value) || tooLate(t0));
                assertEquals(value2, map.get(id));
                assertAllDone();
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
                assertNull(map.putIfAbsent(id, value));
                assertEquals(value, map.putIfAbsent(id, value2));
                assertEquals(value, map.get(id));
            }
        });
    }

    @Test
    public void testPutIfAbsentJustEvicted() throws Exception {
        run("testPutIfAbsentJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertNull(map.putIfAbsent(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertNull(map.putIfAbsent(id, value2));
                assertEquals(value2, map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testPutIfAbsentToBeEvicted() throws Exception {
        run("testPutIfAbsentToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                long t0 = System.nanoTime();
                assertNull(map.putIfAbsent(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.putIfAbsent(id, value2).equals(value) || tooLate(t0));
                assertTrue(map.get(id).equals(value) || tooLate(t0));
                Thread.sleep(evictMs + 1);
                assertNull(map.get(id));
                assertAllDone();
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
    public void testRemoveJustEvicted() throws Exception {
        run("testRemoveJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.remove(id));
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertNull(map.remove(id));
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testRemoveToBeEvicted() throws Exception {
        run("testRemoveToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                assertNull(map.remove(id));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertTrue(map.remove(id).equals(value) || tooLate(t0));
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testRemoveValue() throws Exception {
        run("testRemoveValue", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.remove(id, value));
                assertNull(map.put(id, value));
                assertFalse(map.remove(id, value2)); // miss
                assertTrue(map.remove(id, value)); // hit
                assertNull(map.get(id));
            }
        });
    }

    @Test
    public void testRemoveValueJustEvicted() throws Exception {
        run("testRemoveValueJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.remove(id, value));
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertFalse(map.remove(id, value2)); // miss
                assertFalse(map.remove(id, value)); // hit
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testRemoveValueToBeEvicted() throws Exception {
        run("testRemoveValueToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.remove(id, value));
                long t0 = System.nanoTime();
                assertNull(map.put(id, value, evictMs));
                assertQueueSize(1);
                assertFalse(map.remove(id, value2)); // miss
                assertTrue(map.remove(id, value) || tooLate(t0)); // hit
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testReplace() throws Exception {
        run("testReplace", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertNull(map.replace(id, value));
                assertNull(map.put(id, value));
                assertEquals(value, map.replace(id, value2));
                assertEquals(value2, map.get(id));
            }
        });
    }

    @Test
    public void testReplaceJustEvicted() throws Exception {
        run("testReplaceJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertNull(map.replace(id, value));
                assertNull(map.put(id, value));
                assertEquals(value, map.replace(id, value2, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertNull(map.replace(id, value));
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testReplaceToBeEvicted() throws Exception {
        run("testReplaceToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertNull(map.replace(id, value));
                assertNull(map.put(id, value));
                long t0 = System.nanoTime();
                assertEquals(value, map.replace(id, value2, evictMs));
                assertQueueSize(1);
                assertTrue(map.replace(id, value).equals(value2) || tooLate(t0));
                assertAllDone();
            }
        });
    }

    @Test
    public void testReplaceValue() throws Exception {
        run("testReplaceValue", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.replace(id, value, value2));
                assertNull(map.put(id, value));
                assertFalse(map.replace(id, value2, value)); // miss
                assertTrue(map.replace(id, value, value2)); // hit
                assertEquals(map.get(id), value2);
            }
        });
    }

    @Test
    public void testReplaceValueJustEvicted() throws Exception {
        run("testReplaceValueJustEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.replace(id, value, value2));
                assertNull(map.put(id, value));
                assertTrue(map.replace(id, value, value2, evictMs));
                assertQueueSize(1);
                Thread.sleep(evictMs + 1);
                assertFalse(map.replace(id, value, value)); // miss
                assertFalse(map.replace(id, value2, value)); // hit
                assertNull(map.get(id));
                assertAllDone();
            }
        });
    }

    @Test
    public void testReplaceValueToBeEvicted() throws Exception {
        run("testReplaceValueToBeEvicted", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                String value = getValue(id);
                String value2 = getValue2(id);
                assertFalse(map.replace(id, value, value2));
                assertNull(map.put(id, value));
                long t0 = System.nanoTime();
                assertTrue(map.replace(id, value, value2, evictMs));
                assertQueueSize(1);
                assertFalse(map.replace(id, value, value)); // miss
                assertTrue(map.replace(id, value2, value2) || tooLate(t0)); // hit
                assertAllDone();
            }
        });
    }

    @Test
    public void testClear() throws Exception {
        map.clear();
        assertTrue(map.isEmpty());
        assertNull(map.put(0, getValue(0)));
        assertTrue(!map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testClearWithEviction() throws Exception {
        map.clear();
        assertTrue(map.isEmpty());
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertQueueSize(1);
        assertTrue(!map.isEmpty() || tooLate(t0));
        map.clear();
        assertTrue(map.isEmpty());
        assertAllDone();
    }

    @Test
    public void testKeySet() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Integer> ks = map.keySet();
        assertEquals(m.size(), ks.size());
        assertTrue(ks.containsAll(m.keySet()));
    }

    @Test
    public void testKeySetWithEviction() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Integer> ks = map.keySet();
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertQueueSize(1);
        assertTrue((ks.size() == (m.size() + 1)) || ((ks.size() == m.size()) && tooLate(t0)));
        assertTrue(ks.contains(0) || tooLate(t0));
        Thread.sleep(evictMs + DELAY_MS);
        assertEquals(m.size(), ks.size());
        assertFalse(ks.contains(0));
        assertAllDone();
    }

    @Test
    public void testEntrySet() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Entry<Integer, String>> es = map.entrySet();

        assertFalse(es.isEmpty());
        assertEquals(es.size(), m.size());
        for (Entry<Integer, String> ex : es) {
            assertTrue(es.contains(ex));
        }

        Entry<Integer, String> e = new EvictibleEntry<Integer, String>(map, 4, "4", 0);
        assertFalse(es.contains(e));
        assertTrue(es.add(e));
        assertFalse(es.add(e));
        assertTrue(es.contains(e));
        assertTrue(es.remove(e));
        assertFalse(es.remove(e));
        assertFalse(es.contains(new Object()));
        assertFalse(es.remove(new Object()));

        Entry<Integer, String> e2 = new EvictibleEntry<Integer, String>(map, 1, "", 0);
        assertFalse(es.contains(e2));
        assertFalse(es.add(e2));
        assertFalse(es.remove(e2));

        Entry<Integer, String> e3 = new EvictibleEntry<Integer, String>(map, 1, "1", 0);
        assertTrue(es.contains(e3));
        assertTrue(es.remove(e3));

        Object o = new Object();
        assertFalse(es.contains(o));
        assertFalse(es.remove(o));

        Set<Entry<Integer, String>> es2 = map.entrySet();
        es2.clear();
        assertTrue(es2.isEmpty());
        assertTrue(es.isEmpty());
    }

    @Test
    public void testEntrySetWithEviction() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Entry<Integer, String>> es = map.entrySet();
        EvictibleEntry<Integer, String> e0 = new EvictibleEntry<Integer, String>(map, 0,
            getValue(0), 0);
        long t0 = System.nanoTime();
        assertNull(map.put(0, getValue(0), evictMs));
        assertQueueSize(1);
        assertTrue((es.size() == (m.size() + 1)) || ((es.size() == m.size()) && tooLate(t0)));
        assertTrue(es.contains(e0) || tooLate(t0));
        Thread.sleep(evictMs + DELAY_MS);
        assertEquals(m.size(), es.size());
        assertFalse(es.contains(e0));
        assertAllDone();
    }

    @Test
    public void testEntrySetIterator() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Entry<Integer, String>> es = map.entrySet();
        assertEquals(es.size(), m.size());
        Iterator<Entry<Integer, String>> it = es.iterator();
        assertTrue(it.hasNext());
        Entry<Integer, String> e = it.next();
        assertTrue(es.contains(e));
        it.remove();
        assertEquals(es.size(), m.size() - 1);
        es.clear();
        Iterator<Entry<Integer, String>> it2 = es.iterator();
        assertFalse(it2.hasNext());
    }

    @Test
    public void testEntrySetIterator2() throws Exception {
        Map<Integer, String> m = asMap(Arrays.asList(1, 2, 3));
        map.putAll(m);
        Set<Entry<Integer, String>> es = map.entrySet();
        assertEquals(es.size(), m.size());
        assertFalse(es.contains(new EvictibleEntry<Integer, String>(map, 1, getValue(1), 0)));
        Iterator<Entry<Integer, String>> it = es.iterator();
        assertTrue(it.hasNext());
        while (it.hasNext()) {
            Entry<Integer, String> ex = it.next();
            assertTrue(es.contains(ex));
            ex.setValue(getValue(ex.getKey()));
        }
        assertFalse(it.hasNext());
        assertTrue(es.contains(new EvictibleEntry<Integer, String>(map, 1, getValue(1), 0)));
    }

    @Test(expected = IllegalStateException.class)
    public void testEntrySetIterator3() throws Exception {
        map.putAll(asMap(Arrays.asList(1, 2, 3)));
        Iterator<Entry<Integer, String>> it = map.entrySet().iterator();
        it.remove();
    }

    @Test(expected = IllegalStateException.class)
    public void testEntrySetIterator4() throws Exception {
        map.putAll(asMap(Arrays.asList(1, 2, 3)));
        Iterator<Entry<Integer, String>> it = map.entrySet().iterator();
        it.next();
        it.remove();
        it.remove();
    }

    @Test
    public void testScheduledExpiration() throws Exception {
        int id = 0;
        String value = getValue(id);
        int id2 = getId2(id);
        String value2 = getValue2(id);
        assertTrue(map.isEmpty());
        long t0 = System.nanoTime();
        map.put(id, value, evictMs);
        assertTrue((map.size() == 1) || ((map.size() == 0) && tooLate(t0)));
        assertQueueSize(1);
        long t1 = System.nanoTime();
        map.put(id2, value2, evictMs * 2);
        assertTrue((map.size() == 2) || ((map.size() == 1) && tooLate(t0))
            || ((map.size() == 0) && tooLate(t1, 2)));
        Thread.sleep(evictMs + DELAY_MS);
        assertTrue((map.size() == 1) || ((map.size() == 0) && tooLate(t1, 2)));
        Thread.sleep(evictMs * 2 + DELAY_MS);
        assertEquals(0, map.size());
        assertAllDone();
    }

    private static Map<Integer, String> asMap(List<Integer> values) {
        Map<Integer, String> map = new HashMap<Integer, String>();
        for (int i : values) {
            map.put(i, String.valueOf(i));
        }
        return Collections.unmodifiableMap(map);
    }

    private void clearEvictionExecutorQueue() {
        if (numThreads == 1 && evictionExecutor != null) {
            evictionExecutor.getQueue().clear();
        }
    }

    private boolean tooLate(long t) {
        return tooLate(t, 1);
    }

    private boolean tooLate(long t, long factor) {
        long elapsed = System.nanoTime() - t;
        System.out.printf("Too late: %f ms\n", ((double) elapsed / 1000000.0));
        return (elapsed > NANOSECONDS.convert(evictMs * factor, MILLISECONDS));
    }

    private void assertQueueSize(int expected) {
        if (numThreads == 1 && evictionExecutor != null) {
            assertEquals(expected, evictionExecutor.getQueue().size());
        }
    }

    private void assertAllDone() throws InterruptedException {
        if (numThreads == 1 && evictionExecutor != null) {
            Thread.sleep(1);
            for (Runnable runnable : evictionExecutor.getQueue()) {
                if ((runnable != null) && (runnable instanceof ScheduledFuture)) {
                    assertTrue(((ScheduledFuture<?>) runnable).isDone());
                }
            }
        }
    }

}
