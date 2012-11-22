Thank you for considering my submission for the Cayetano programming contest.

My solution for the Java task is Evictor, a Java library providing an implementation of `java.util.concurrent.ConcurrentMap` that supports timed entry eviction, as specified in the task definition. You can find more information in the provided README and Javadoc. In this document, I would like to provide some additional information that could help you test and evaluate my solution.

I started with the simple idea of providing a `ConcurrentMap` decorator, but ended up with a mini-library which contains three different interfaces with multiple implementations for each one of them. The main reason is that I experimented with different approaches for scheduling the automated eviction and came up with several ideas, three of which seem to perform roughly equally so I am not able to decide which one is best. Furthermore, I expect that each may deliver better performance than the other two depending on the way the map is actually used (see README).

# Testing

Due to the above reasons, I would like to ask you to test at least the following three implementations:

1. `ConcurrentHashMapWithTimedEviction` with `RegularTaskEvictionScheduler` and optimal scheduling delay, initial capacity, and concurrency level. 

+ The optimal delay depends on the acceptable accuracy. If it's acceptable for the automated evictions to happen with a lower average accuracy than 1 ms, please specify it, e.g. as 2 or 3 milliseconds, this would positively impact performance.
+ The optimal initial capacity depends on the expected size of the map. If the map is expected to hold 1,000,000 entries, it is better to specify this value in the constructor, as this would eliminate any resizing and therefore the performance would be higher.
+ The optimal concurrency level should be equal to the number of threads accessing the map at the same time, according to the JDK documentation.

```
EvictionScheduler<Integer, String> scheduler = new RegularTaskEvictionScheduler<>(delay, TimeUnit.MILLISECONDS);
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>(initialCapacity, 0.75f, concurrencyLevel, scheduler);
```

2. `ConcurrentHashMapWithTimedEviction` with `DelayedTaskEvictionScheduler` and optimal initial capacity and concurrency level (see above).

```
EvictionScheduler<Integer, String> scheduler = new DelayedTaskEvictionScheduler<>();
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>(initialCapacity, 0.75f, concurrencyLevel, scheduler);
```

3. `ConcurrentHashMapWithTimedEviction` with `SingleThreadEvictionScheduler` and optimal initial capacity and concurrency level (see above).

```
EvictionScheduler<Integer, String> scheduler = new SingleThreadEvictionScheduler<>();
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>(initialCapacity, 0.75f, concurrencyLevel, scheduler);
```

Please test with JDK 7 if possible. I tested mainly with this version of the JDK. I did some testing with JDK 6 as well and I believe I observed lower performance than with JDK 7. Currently, the pre-compiled package is binary compatible with JDK 7. The source is compatible with JDK 6 and could be recompiled and tested with JDK 6 if needed.

# Design Considerations

## Interfaces

As you may have noticed, there are some things I did a bit differently than requested, I believe for a good reason:

+ I designed a parameterized map class `<K, V>`, which I believe is more useful than a map accepting `Object` as key or value.
+ My main interface `ConcurrentMapWithTimedEviction` extends `java.util.concurrent.ConcurrentMap`, which from my perspective again is very useful. This however forced me to implement many more methods than requested.
+ Besides the methods `put` and `putIfAbsent`, I added also `evictMs` versions of the 2 `replace` methods in `ConcurrentMap`. Since my interface extends `ConcurrentMap`, it is natural all methods that can put something in the map to have `evictMs` versions.

## Composability

I favored composability heavily and therefore as a user of the library, you have the flexibility to decorate a different class than the obvious `java.util.concurrent.ConcurrentHashMap` or to choose a different (e.g. faster) priority queue implementation in the eviction schedulers than `java.util.concurrent.ConcurrentSkipListMap`. In practice, I was not able to find neither a faster concurrent hash map than `ConcurrentHashMap`, nor a faster concurrent priority queue than `ConcurrentSkipListMap`. However, if such **would** exist, they could easily be used with the library.

## Thread Safety

Thread safety is ensured via the following simple approach:

+ All used data structures are thread safe, e.g. `java.util.concurrent.ConcurrentHashMap`
+ All class fields are either `final` (when possible) or `volatile`
+ A small number of private methods in the schedulers are `synchronized` to ensure atomicity in a concurrent environment

I considered using some of the new concurrency primitives available since Java 6, but eventually decided that in this particular case, they offer no advantage over `volatile` and `synchronized`.

# Tests

There are three test classes provided with the library:

+ `ConcurrentMapWithTimedEvictionTest` tests the functional correctness of the different `ConcurrentMapWithTimedEviction` implementations in single-threaded and multi-threaded environment.
+ `ConcurrentMapWithTimedEvictionAccuracyTest` tests the *accuracy* of the different eviction schedulers, i.e. how close are the actual eviction times to the times specified via the `evictMs` parameter. Here, an average accuracy of 400-600 microseconds is considered optimal. 
+ `ConcurrentMapWithTimedEvictionPerfTest` tests the performance of the different `ConcurrentMapWithTimedEviction` implementations under an artificially heavy load

Note that `ConcurrentMapWithTimedEvictionTest` sometimes (but very rarely) fails due to unpredictible lower accuracy of single evictions. The test could be designed in a different way to avoid this, e.g. by mocking the system timer, but since the current approach worked for me I didn't do it. 

# Build / Development Environment

There is a Maven build which you could use to rebuild the library if needed. Just do `mvn clean install`, or `mvn clean install -DskipTests` if you would rather skip the tests. The only external dependencies used in the project are JUnit and Guava, and these are used only for testing. The actual jar built has no external dependencies whatsoever. Besides the binary package, the Maven build produces also source and Javadoc packages.

There is also an Eclipse project. If you import it in Eclipse, make sure that you have the Maven Eclipse plugin (m2e) installed.
