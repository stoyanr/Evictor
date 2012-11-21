Overview
========

**Evictor** is a Java library providing a composable implementation of `java.util.concurrent.ConcurrentMap` that supports timed entry eviction. 

The central abstraction is the interface `ConcurrentMapWithTimedEviction`, which extends `ConcurrentMap` by adding the following four methods:

```
V put(K key, V value, long evictMs);
V putIfAbsent(K key, V value, long evictMs);
V replace(K key, V value, long evictMs);
boolean replace(K key, V oldValue, V newValue, long evictMs);
```

In the above methods, `evictMs` is the time in milliseconds during which the entry can stay in the map (time-to-live). When this time has elapsed, the entry will be evicted from the map automatically. A value of 0 means "forever".

There is a single implementation of this interface, `ConcurrentMapWithTimedEvictionDecorator`, which decorates an existing `ConcurrentMap` implementation, and one convenient subclass, `ConcurrentHashMapWithTimedEviction` which conforms to the `ConcurrentHashMap` specification and is easier to use than its superclass if a `ConcurrentHashMap` is what you want. These two classes can be customized with different *eviction schedulers*, which is an abstraction for the actual mechanism to automatically evict entries upon expiration. In addition, some of the schdulers are based on a priority queue and can be additionally customized by using different priority queue implementations.

The library has the following features:

* **Ease of use** - just use the default `ConcurrentHashMapWithTimedEviction` constructor if you don't care about the details.
* **Extreme composability** - if you do care about the details, you can supply your own `ConcurrentMap` implementation, choose among the 4 available `EvictionScheduler` implementations (or supply your own), and among the 2 available `EvictionQueue` implementations (or supply your own) to create a map which has an even higher performance or is tuned to your needs.
* **Thread safety** - all classes are safe to use in a concurrent environment
* **High performance** - the library makes minimal use of locking and provides several optimized eviction strategies
* **Detailed documentation** - there are very comprehensive Javadoc, class diagram, and README
* **Clean code** - if you need to read the code, you are welcome, it does "read like a well-written prose"

Basic Usage
===========

Creating a ConcurrentHashMap
----------------------------

To create a new concurrent hash map with timed entry eviction which conforms to the `ConcurrentHashMap` specification, you can use the class `ConcurrentHashMapWithTimedEviction`. You can create instances of this class using either its default constructor, which supplies default values for all arguments, or one of its overloaded constructors:

```
// Create hash map with default initial capacity, load factor, number of threads, and eviction scheduler
// An instance of SingleThreadEvictionScheduler is used in this case
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>();

// Create delayed task eviction scheduler
EvictionScheduler<Integer, String> scheduler = new DelayedTaskEvictionScheduler<>();
// Create hash map with default initial capacity, load factor, and concurrency level, 
// and the previously created scheduler
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>(scheduler);

// Create regular task eviction scheduler with a delay of 750 microseconds
EvictionScheduler<Integer, String> scheduler = new RegularTaskEvictionScheduler<>(750, TimeUnit.MICROSECONDS);
// Create hash map with the specified initial capacity, load factor, and concurrency level, 
// and the previously created scheduler
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentHashMapWithTimedEviction<>(100, 0.75f, 8, scheduler);
```

Decorating a Custom ConcurrentMap Implementation
------------------------------------------------

The class `ConcurrentHashMapWithTimedEviction` inherits from `ConcurrentMapWithTimedEvictionDecorator` and supplies a `ConcurrentHashMap` as a delegate. If you would like to pass a different `ConcurrentMap` implementation, you could use the `ConcurrentMapWithTimedEvictionDecorator` class directlry. For example, here is how you could use Google Guava to create the underlying map:

```
// Create a concurrent hash map with Guava
ConcurrentMap<Integer, EvictibleEntry<Integer, String>> delegate = new MapMaker().makeMap();
// Create a map with a SingleThreadEvictionScheduler
EvictionScheduler<Integer, String> scheduler = new SingleThreadEvictionScheduler<>();
ConcurrentMapWithTimedEviction<Integer, String> map = new ConcurrentMapWithTimedEvictionDecorator<>(delegate, scheduler);
```

Eviction Schedulers
-------------------

There are four different types of eviction schedulers that can be used with `ConcurrentMapWithTimedEvictionDecorator` and its subclass `ConcurrentHashMapWithTimedEviction`: 

* `ExecutorServiceEvictionScheduler` uses a `java.util.concurrent.ScheduledExecutorService` to schedule multiple tasks for entries that should be evicted, one task per entry.
* `RegularTaskEvictionScheduler` uses a priority queue to store entries in the order in which they should be evicted, and a regular task scheduled with a fixed delay in an `ScheduledExecutorService` to manage the automated eviction.
* `DelayedTaskEvictionScheduler` is also based on a priority queue, and uses a single delayed task scheduled in an `ScheduledExecutorService` to manage the  automated eviction. The task is rescheduled appropriately each time an entry is added or removed from the queue to ensure that it will always fire at the time of the next scheduled eviction, not sooner or later.
* `SingleThreadEvictionScheduler` is also based on a priority queue, and uses a single thread to manage the automated eviction. The behavior is similar to that of `DelayedTaskEvictionScheduler`, but it is implemented at a lower level, using a specially crafted thread rather than a scheduled executor service.

For all schedulers that use `ScheduledExecutorService` (the first three), you can pass an implementation upon construction. If you don't pass anything, an instance of `java.util.concurrent.ScheduledThreadPoolExecutor` with 1 thread is created and used. Similarly, for all queue-based schedulers (the last three), you can pass the queue upon construction, and an instance of `NavigableMapEvictionQueue` is used as a default.

Regarding performance, under heavy contention the last three priority queue-based schedulers (with their default queues) tend to outperform the first one by a factor of two or three, especially regarding `put` performance. These three schedulers have roughly equal performance characteristics, which also depend on the actual map usage.

Note that you can create a single eviction scheduler and use it with multiple maps.

Eviction Queues
---------------

There are two different types of eviction queues that can be used with the three queue-based eviction schedulers described above:

* `NavigableMapEvictionQueue` uses a `java.util.concurrent.ConcurrentNavigableMap` to store its entries. The key in the map is the eviction time of the entry, and the value is the entry itself.
* `PriorityEvictionQueue` uses a `java.util.Queue` to store its entries. The queue should support priority queue semantics and be thread-safe.

For both queue types, the actual navigable map or queue implementation can be passed upon construction. By default, `NavigableMapEvictionQueue` uses an instance of `java.util.concurrent.ConcurrentSkipListMap`, and `PriorityEvictionQueue` uses an instance of `java.util.concurrent.PriorityBlockingQueue`.

Regarding performance, `NavigableMapEvictionQueue` with its default map significantly outperforms `PriorityEvictionQueue` with its default queue. Therefore, all three schedulers mentioned above use `NavigableMapEvictionQueue` by default. Normally, you would not need to change this, unless you have a priority queue implementation which can outpeform `java.util.concurrent.ConcurrentSkipListMap` in a concurrent environment. In this case, you could implement the `EvictionQueue` interface yourself and pass it to the appropriate scheduler.

Design
======

For a more comprehensive overview of the library design, see the provided class diagram:

![Design](design/design.png "Design")

Tests
=====

There are three test classes provided with the library:

* `ConcurrentMapWithTimedEvictionTest` tests the functional correctness of the different `ConcurrentMapWithTimedEviction` implementations in single-threaded and multi-threaded environment.
* `ConcurrentMapWithTimedEvictionAccuracyTest` tests the *accuracy* of the different eviction schedulers, i.e. how close are the actual eviction times to the times specified via the `evictMs` parameter. Here, an average accuracy of 400-500 microseconds is considered optimal. With some schedulers (for example `RegularTaskEvictionScheduler`), you can supply a parameter upon construction which lowers their accuracy, effectively trading it for performance.
* `ConcurrentMapWithTimedEvictionAccuracyPerfTest` tests the performance of the different `ConcurrentMapWithTimedEviction` implementations under an artificially heavy load (100 threads, 1,000,000 and 

Performance
===========
