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

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.stoyanr.evictor.ConcurrentMapWithTimedEviction;

/**
 * Unit tests for performance
 * 
 * @author Stoyan Rachev
 * @author sangupta
 *
 */
@RunWith(value = Parameterized.class)
public class ConcurrentMapWithTimedEvictionPerfTest extends
    AbstractConcurrentMapWithTimedEvictionTest {

    private static final int NUM_THREADS = 100;
    private static final int NUM_ITERATIONS = 20000;
    private static final int EVICT_MS = 200;

    @Parameters
    public static Collection<Object[]> data() {
        // @formatter:off
        return Arrays.asList(new Object[][] {
            { IMPL_CHM }, 
//            { IMPL_GUAVA_CACHE }, 
            { IMPL_GUAVA_CACHE_E }, 
//            { IMPL_CHMWTE_NULL }, 
            { IMPL_CHMWTE_ESS }, 
            { IMPL_CHMWTE_NM_RT },
            { IMPL_CHMWTE_NM_DT },
            { IMPL_CHMWTE_NM_ST },
//            { IMPL_CHMWTE_PQ_ST }
        });
        // @formatter:on
    }

    public ConcurrentMapWithTimedEvictionPerfTest(int impl) {
        super(impl, EVICT_MS, NUM_THREADS, NUM_ITERATIONS);
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
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
                mapx.put(id, getValue(id), getEvictMs());
            }
        });
    }

    @Ignore
    @Test
    public void testGetAndPutWithEviction() throws Exception {
        if (!(map instanceof ConcurrentMapWithTimedEviction))
            return;
        final ConcurrentMapWithTimedEviction<Integer, String> mapx = (ConcurrentMapWithTimedEviction<Integer, String>) map;
        run("testGetAndPutWithEviction", new TestTask() {
            @Override
            public void test(int id) throws InterruptedException {
                if (id % 2 == 0) {
                    mapx.put(id, getValue(id), getEvictMs());
                } else {
                    mapx.get(MAX_MAP_SIZE - id - 1);
                }
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
                map.put(id, getValue(id), getEvictMs());
            }
        }
    }

    private long getEvictMs() {
//        return (long) (Math.random() * evictMs);
        return evictMs;
    }

}
