Overview
========

**Evictor** is a Java library providing an implementation of `java.util.concurrent.ConcurrentMap` that supports timed entry eviction. 

The central abstraction is the interface `ConcurrentMapWithTimedEviction`, which extends `ConcurrentMap` by adding the following four methods:

```
V put(K key, V value, long evictMs);
V putIfAbsent(K key, V value, long evictMs);
V replace(K key, V value, long evictMs);
boolean replace(K key, V oldValue, V newValue, long evictMs);
```

In the above methods, `evictMs` is the time in milliseconds during which the entry can stay in the map (time-to-live). When this time has elapsed, the entry will be evicted from the map automatically. A value of 0 means "forever".

There is a single implementation of this interface, `ConcurrentMapWithTimedEvictionDecorator`, which decorates an existing `ConcurrentMap` implementation, and one convenient subclass, `ConcurrentHashMapWithTimedEviction` which conforms to the `ConcurrentHashMap` specification and is easier to use than its superclass if a `ConcurrentHashMap` is what you want. These two classes can be customized with different *eviction schedulers*, which is an abstraction for the actual mechanism to automatically evict entries upon expiration. In addition, some of the schedulers are based on a priority queue and can be additionally customized by using different priority queue implementations.

The library has the following features:

+ **Ease of use** - just use the default `ConcurrentHashMapWithTimedEviction` constructor if you don't care about the details.
+ **Extreme composability** - if you do care about the details, you can supply your own `ConcurrentMap` implementation, choose among the 4 available `EvictionScheduler` implementations (or supply your own), and among the 2 available `EvictionQueue` implementations (or supply your own) to create a map which has an even higher performance or is tuned to your needs.
+ **Thread safety** - all classes are safe to use in a concurrent environment
+ **High performance** - higher performance than common alternatives such as [Google Guava](http://code.google.com/p/guava-libraries/) by minimal use of locking and optimized eviction implementations
+ **Detailed documentation** - there are very comprehensive Javadoc, class diagram, and README
+ **Clean code** - if you need to read the code, you are welcome, it does "read like a well-written prose"

Basic Usage
===========

Creating a ConcurrentHashMapWithTimedEviction
---------------------------------------------

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

The class `ConcurrentHashMapWithTimedEviction` inherits from `ConcurrentMapWithTimedEvictionDecorator` and supplies a `ConcurrentHashMap` as a delegate. If you would like to pass a different `ConcurrentMap` implementation, you could use the `ConcurrentMapWithTimedEvictionDecorator` class directly. For example, here is how you could use Google Guava to create the underlying map:

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

+ `ExecutorServiceEvictionScheduler` uses a `java.util.concurrent.ScheduledExecutorService` to schedule multiple tasks for entries that should be evicted, one task per entry.
+ `RegularTaskEvictionScheduler` uses a priority queue to store entries in the order in which they should be evicted, and a regular task scheduled with a fixed delay in an `ScheduledExecutorService` to manage the automated eviction.
+ `DelayedTaskEvictionScheduler` is also based on a priority queue, and uses a single delayed task scheduled in an `ScheduledExecutorService` to manage the  automated eviction. The task is rescheduled appropriately each time an entry is added or removed from the queue to ensure that it will always fire at the time of the next scheduled eviction, not sooner or later.
+ `SingleThreadEvictionScheduler` is also based on a priority queue, and uses a single thread to manage the automated eviction. The behavior is similar to that of `DelayedTaskEvictionScheduler`, but it is implemented at a lower level, using a specially crafted thread rather than a scheduled executor service.

For all schedulers that use `ScheduledExecutorService` (the first three), you can pass an implementation upon construction. If you don't pass anything, an instance of `java.util.concurrent.ScheduledThreadPoolExecutor` with 1 thread is created and used. Similarly, for all queue-based schedulers (the last three), you can pass the queue upon construction, and an instance of `NavigableMapEvictionQueue` is used as a default.

Regarding performance, under heavy contention the last three queue-based schedulers (with their default queues) tend to outperform the first one by a factor of two or three, especially regarding `put` performance. These three schedulers have roughly equal performance characteristics, which also depend on the actual map usage.

Note that you can create a single eviction scheduler and use it with multiple maps.

Eviction Queues
---------------

There are two different types of eviction queues that can be used with the three queue-based eviction schedulers described above:

+ `NavigableMapEvictionQueue` uses a `java.util.concurrent.ConcurrentNavigableMap` to store its entries. The key in the map is the eviction time of the entry, and the value is the entry itself.
+ `PriorityEvictionQueue` uses a `java.util.Queue` to store its entries. The queue should support priority queue semantics and be thread-safe.

For both queue types, the actual navigable map or queue implementation can be passed upon construction. By default, `NavigableMapEvictionQueue` uses an instance of `java.util.concurrent.ConcurrentSkipListMap`, and `PriorityEvictionQueue` uses an instance of `java.util.concurrent.PriorityBlockingQueue`.

Regarding performance, `NavigableMapEvictionQueue` with its default map significantly outperforms `PriorityEvictionQueue` with its default queue. Therefore, all three schedulers mentioned above use `NavigableMapEvictionQueue` by default. Normally, you would not need to change this, unless you have a priority queue implementation which can outperform `java.util.concurrent.ConcurrentSkipListMap` in a concurrent environment. In this case, you could implement the `EvictionQueue` interface yourself and pass it to the appropriate scheduler.

Performance
===========

The table below compares the performance of the different approaches, benchmarked against the JDK `ConcurrentHashMap` (non-evicting) and Guava's `Cache` (evicting). The results shown are the typical times (in microseconds) to execute a single Get or Put operation, as measured by multiple executions of a specially designed test in the following environment:

+ CPU Intel i7-2600 3.4 GHz (8 cores), 16 GB RAM, Windows 7, JDK 7u9
+ 100 threads performing 2,000,000 operations on a map containing max 1,000,000 entries
+ Constant eviction time of 200 milliseconds, roughly equal to the half of the time needed to execute the fastest tests. Guava does not support variable eviction times per put, so with variable times the results would not be comparable.
+ For the three queue-based scheduler, the default queue implementation is used, which is `NavigableMapEvictionQueue` with a `java.util.concurrent.ConcurrentSkipListMap`.
+ The delay of `RegularTaskEvictionScheduler` is set to 750 microseconds, which makes it as accurate as the other schedulers (400-600 microseconds), but negatively impacts performance compared to the default of 1 millisecond.

Implementation                                                                Get (us)   Put (us) 
---------------------------------------------------------------------------- --------- ---------- 
`ConcurrentHashMap` (JDK, non-evicting)                                      0.1 - 2.0     5 - 15
`Cache` (Guava, evicting)                                                    0.3 - 0.5    40 - 45
`ConcurrentHashMapWithTimedEviction` with `ExecutorServiceEvictionScheduler` 0.1 - 1.5   90 - 180
`ConcurrentHashMapWithTimedEviction` with `RegularTaskEvictionScheduler`     0.1 - 1.5    10 - 20
`ConcurrentHashMapWithTimedEviction` with `DelayedTaskEvictionScheduler`     0.1 - 0.7     9 - 11
`ConcurrentHashMapWithTimedEviction` with `SingleThreadEvictionScheduler`    0.1 - 0.7     9 - 11

As can be seen from the above table, `ExecutorServiceEvictionScheduler` delivers the worst Put performance, with Guava coming next and the three queue-based eviction schedulers clearly outperforming Guava. There is no clear winner among the last three. Each of them would deliver optimal performance in different circumstances, for example:

+ `RegularTaskEvictionScheduler` has very fast scheduling and cancellation but executes its eviction task at a regular interval, potentially without need. This implementation would deliver optimal performance if scheduling and cancellation are made often and entries expire also often, roughly every 1-10 milliseconds. 
+ `DelayedTaskEvictionScheduler` and `SingleThreadEvictionScheduler` are slower during scheduling and cancellation, but the eviction task will run exactly at the time of the next scheduled eviction, not sooner or later. These implementations would deliver optimal performance if scheduling and cancellation are made less often and entries expire at irregular intervals ranging from 1 to hundreds milliseconds.

Between `DelayedTaskEvictionScheduler` and `SingleThreadEvictionScheduler`, one would expect that `SingleThreadEvictionScheduler` would deliver better performance, since it is a lower-level minimalistic implementation. However, this is not yet confirmed by testing. It seems cancellation and scheduling in a `ScheduledExecutorService` are really cheap and the two schedulers perform roughly equally.

In addition, `RegularTaskEvictionScheduler` has the advantage over the other two that by specifying a higher delay than 1 millisecond you could trade lower eviction accuracy for higher performance, as its regular task would then fire less often.

Design
======

For a more comprehensive overview of the library design, see the provided class diagram:

![Design](design/design.png)

