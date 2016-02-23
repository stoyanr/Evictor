# Evictor - A concurrent map with timed element eviction

[![Build Status](https://travis-ci.org/sangupta/Evictor.svg?branch=master)](https://travis-ci.org/sangupta/Evictor)
[![Coverage Status](https://coveralls.io/repos/sangupta/Evictor/badge.png)](https://coveralls.io/r/sangupta/Evictor)


## Changes from original project: github:stoyanr/Evictor

Following changes have been made/are to be made to this fork:

* Default JDK version is now 1.7
* OSS-Mavenized the project so that it is available via Maven Central
* Separated inner classes
* Modified code for better readability and checks
* Removed eclipse-specific files
* Removed the module - the top-level POM is the project POM
* Improved exception messages
* Some code formatting
* Simple travis-ci.org and coveralls.io integration


## Introduction

**Evictor** is a Java library providing an implementation of `java.util.concurrent.ConcurrentMap` that supports timed entry eviction for caching. It is easy to use, thread-safe, very fast, and highly composable. It actually won a [programming contest](http://www.cayetanogaming.com/javatask) in which the submissions were judged for thread safety, performance, and design.

You can download the latest [binary](http://stoyanr.github.com/Evictor/evictor/lib/evictor-1.0.jar), [javadoc](http://stoyanr.github.com/Evictor/evictor/lib/evictor-1.0-javadoc.jar), and [sources](http://stoyanr.github.com/Evictor/evictor/lib/evictor-1.0-sources.jar) and browse the [Javadoc online](http://stoyanr.github.com/Evictor/evictor/javadoc/). 

This work is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

## Overview

The central abstraction is the interface `ConcurrentMapWithTimedEviction`, which extends `ConcurrentMap` by adding the following four methods:

```java
V put(K key, V value, long evictMs);
V putIfAbsent(K key, V value, long evictMs);
V replace(K key, V value, long evictMs);
boolean replace(K key, V oldValue, V newValue, long evictMs);
```

In the above methods, `evictMs` is the time in milliseconds during which the entry can stay in the map (time-to-live). When this time has elapsed, the entry will be evicted from the map automatically. A value of 0 means "forever".

There is a single implementation of this interface, `ConcurrentMapWithTimedEvictionDecorator`, which decorates an existing `ConcurrentMap` implementation, and one convenient subclass, `ConcurrentHashMapWithTimedEviction` which conforms to the `ConcurrentHashMap` specification and is easier to use than its superclass if a `ConcurrentHashMap` is what you want. These two classes can be customized with different *eviction schedulers*, which is an abstraction for the actual mechanism to automatically evict entries upon expiration. In addition, some of the schedulers are based on a priority queue and can be additionally customized by using different priority queue implementations.

### Library Features

* **Ease of use** - just use the default `ConcurrentHashMapWithTimedEviction` constructor if you don't care about the details.
* **Extreme composability** - if you do care about the details, you can supply your own `ConcurrentMap` implementation, choose among the 4 available `EvictionScheduler` implementations (or supply your own), and among the 2 available `EvictionQueue` implementations (or supply your own) to create a map which has an even higher performance or is tuned to your needs.
* **Thread safety** - all classes are safe to use in a concurrent environment
* **High performance** - higher performance than common alternatives such as [Google Guava](http://code.google.com/p/guava-libraries/) by minimal use of locking and optimized eviction implementations
* **Detailed documentation** - there are very comprehensive Javadoc, class diagram, and README
* **Clean code** - if you need to read the code, you are welcome, it does "read like a well-written prose"

### History

As already mentioned, Evictor was originally created in November 2012 as a submission for a [programming contest](http://www.cayetanogaming.com/javatask) sponsored and organized by [Cayetano Gaming](http://www.cayetanogaming.com/) and announced at the [Java2Days conference](http://2012.java2days.com/?page_id=36). Eventually, Evictor actually *won* this contest, so I deemed it worthy of sharing with the community.

The contest task requested simply to design a concurrent (thread-safe) map that supports timed entry eviction, having most of the standard map operations and overloaded versions of the `put` and `putIfAbsent` accepting one additional argument, the time-to-live in milliseconds. The criteria to judge the solutions included thread safety, performance, and design.

I started with the simple idea of providing a `java.util.concurrent.ConcurrentMap` decorator, but ended up with a mini-library containing three different interfaces with multiple implementations for each one of them. I experimented with different approaches for scheduling the automated eviction and came up with several ideas, three of which seemed to perform roughly equally so I was not able to decide which one is best. Furthermore, I determined that each may deliver better performance than the other two depending on the way the map is actually used.

## Usage

### Creating a ConcurrentHashMapWithTimedEviction

To create a new concurrent hash map with timed entry eviction which conforms to the `ConcurrentHashMap` specification, you can use the class `ConcurrentHashMapWithTimedEviction`. You can create instances of this class using either its default constructor, which supplies default values for all arguments, or one of its overloaded constructors:

```java
// Create hash map with default initial capacity, load factor, number of threads,
// and eviction scheduler
// An instance of SingleThreadEvictionScheduler is used in this case
ConcurrentMapWithTimedEviction<Integer, String> map = 
    new ConcurrentHashMapWithTimedEviction<>();

// Create delayed task eviction scheduler
EvictionScheduler<Integer, String> scheduler = new DelayedTaskEvictionScheduler<>();
// Create hash map with default initial capacity, load factor, and concurrency level, 
// and the previously created scheduler
ConcurrentMapWithTimedEviction<Integer, String> map = 
    new ConcurrentHashMapWithTimedEviction<>(scheduler);

// Create regular task eviction scheduler with a delay of 750 microseconds
EvictionScheduler<Integer, String> scheduler = 
    new RegularTaskEvictionScheduler<>(750, TimeUnit.MICROSECONDS);
// Create hash map with the specified initial capacity, load factor, and concurrency
// level, and the previously created scheduler
ConcurrentMapWithTimedEviction<Integer, String> map = 
    new ConcurrentHashMapWithTimedEviction<>(100, 0.75f, 8, scheduler);
```
See:
* [ConcurrentMapWithTimedEviction.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/ConcurrentMapWithTimedEviction.java)
* [ConcurrentHashMapWithTimedEviction.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/ConcurrentHashMapWithTimedEviction.java)

### Decorating a Custom ConcurrentMap Implementation

The class `ConcurrentHashMapWithTimedEviction` inherits from `ConcurrentMapWithTimedEvictionDecorator` and supplies a `ConcurrentHashMap` as a delegate. If you would like to pass a different `ConcurrentMap` implementation, you could use the `ConcurrentMapWithTimedEvictionDecorator` class directly. For example, here is how you could use Google Guava to create the underlying map:

```java
// Create a concurrent hash map with Guava
ConcurrentMap<Integer, EvictibleEntry<Integer, String>> delegate = 
    new MapMaker().makeMap();
// Create a map with a SingleThreadEvictionScheduler
EvictionScheduler<Integer, String> scheduler = new SingleThreadEvictionScheduler<>();
ConcurrentMapWithTimedEviction<Integer, String> map = 
    new ConcurrentMapWithTimedEvictionDecorator<>(delegate, scheduler);
```

See:
* [ConcurrentMapWithTimedEvictionDecorator.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/ConcurrentMapWithTimedEvictionDecorator.java)

### Eviction Schedulers

There are four different types of eviction schedulers that can be used with `ConcurrentMapWithTimedEvictionDecorator` and its subclass `ConcurrentHashMapWithTimedEviction`: 

* `ExecutorServiceEvictionScheduler` uses a `java.util.concurrent.ScheduledExecutorService` to schedule multiple tasks for entries that should be evicted, one task per entry.
* `RegularTaskEvictionScheduler` uses a priority queue to store entries in the order in which they should be evicted, and a regular task scheduled with a fixed delay in an `ScheduledExecutorService` to manage the automated eviction.
* `DelayedTaskEvictionScheduler` is also based on a priority queue, and uses a single delayed task scheduled in an `ScheduledExecutorService` to manage the  automated eviction. The task is rescheduled appropriately each time an entry is added or removed from the queue to ensure that it will always fire at the time of the next scheduled eviction, not sooner or later.
* `SingleThreadEvictionScheduler` is also based on a priority queue, and uses a single thread to manage the automated eviction. The behavior is similar to that of `DelayedTaskEvictionScheduler`, but it is implemented at a lower level, using a specially crafted thread rather than a scheduled executor service.

For all schedulers that use `ScheduledExecutorService` (the first three), you can pass an implementation upon construction. If you don't pass anything, an instance of `java.util.concurrent.ScheduledThreadPoolExecutor` with 1 thread is created and used. Similarly, for all queue-based schedulers (the last three), you can pass the queue upon construction, and an instance of `NavigableMapEvictionQueue` is used as a default.

Regarding performance, under heavy contention the last three queue-based schedulers (with their default queues) tend to outperform the first one by a factor of two or three, especially regarding `put` performance. These three schedulers have roughly equal performance characteristics, which also depend on the actual map usage.

Note that you can create a single eviction scheduler and use it with multiple maps.

See:
* [EvictionScheduler.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/EvictionScheduler.java)
* [ExecutorServiceEvictionScheduler.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/ExecutorServiceEvictionScheduler.java)
* [RegularTaskEvictionScheduler.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/RegularTaskEvictionScheduler.java)
* [DelayedTaskEvictionScheduler.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/DelayedTaskEvictionScheduler.java)
* [SingleThreadEvictionScheduler.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/SingleThreadEvictionScheduler.java)

### Eviction Queues

There are two different types of eviction queues that can be used with the three queue-based eviction schedulers described above:

* `NavigableMapEvictionQueue` uses a `java.util.concurrent.ConcurrentNavigableMap` to store its entries. The key in the map is the eviction time of the entry, and the value is the entry itself.
* `PriorityEvictionQueue` uses a `java.util.Queue` to store its entries. The queue should support priority queue semantics and be thread-safe.

For both queue types, the actual navigable map or queue implementation can be passed upon construction. By default, `NavigableMapEvictionQueue` uses an instance of `java.util.concurrent.ConcurrentSkipListMap`, and `PriorityEvictionQueue` uses an instance of `java.util.concurrent.PriorityBlockingQueue`.

Regarding performance, `NavigableMapEvictionQueue` with its default map significantly outperforms `PriorityEvictionQueue` with its default queue. Therefore, all three schedulers mentioned above use `NavigableMapEvictionQueue` by default. Normally, you would not need to change this, unless you have a priority queue implementation which can outperform `java.util.concurrent.ConcurrentSkipListMap` in a concurrent environment. In this case, you could implement the `EvictionQueue` interface yourself and pass it to the appropriate scheduler.

See:
* [EvictionQueue.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/EvictionQueue.java)
* [NavigableMapEvictionQueue.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/NavigableMapEvictionQueue.java)
* [PriorityEvictionQueue.java](Evictor/blob/master/evictor/src/com/stoyanr/evictor/PriorityEvictionQueue.java)

## Performance

The table below compares the performance of the different approaches, benchmarked against the JDK `ConcurrentHashMap` (non-evicting) and Guava's `Cache` (evicting). The results shown are the typical times (in microseconds) to execute a single Get or Put operation, as measured by multiple executions of a specially designed test in the following environment:

* CPU Intel i7-2600 3.4 GHz (8 cores), 16 GB RAM, Windows 7, JDK 7u9
* 100 threads performing 2,000,000 operations on a map containing max 1,000,000 entries
* Constant eviction time of 200 milliseconds, roughly equal to the half of the time needed to execute the fastest tests. Guava does not support variable eviction times per put, so with variable times the results would not be comparable.
* For the three queue-based scheduler, the default queue implementation is used, which is `NavigableMapEvictionQueue` with a `java.util.concurrent.ConcurrentSkipListMap`.
* The delay of `RegularTaskEvictionScheduler` is set to 750 microseconds, which makes it as accurate as the other schedulers (400-600 microseconds), but negatively impacts performance compared to the default of 1 millisecond.

<table border="1" cellpadding="4" cellspacing="0">
<thead>
<tr class="header">
<th align="left">Implementation</th>
<th align="right">Get (us)</th>
<th align="right">Put (us)</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left"><code>ConcurrentHashMap</code> (JDK, non-evicting)</td>
<td align="right">0.1 - 1.0</td>
<td align="right">5 - 15</td>
</tr>
<tr class="even">
<td align="left"><code>Cache</code> (Guava, evicting)</td>
<td align="right">0.3 - 0.5</td>
<td align="right">40 - 45</td>
</tr>
<tr class="odd">
<td align="left"><code>ConcurrentHashMapWithTimedEviction</code> with <code>ExecutorServiceEvictionScheduler</code></td>
<td align="right">0.1 - 1.5</td>
<td align="right">90 - 180</td>
</tr>
<tr class="even">
<td align="left"><code>ConcurrentHashMapWithTimedEviction</code> with <code>RegularTaskEvictionScheduler</code></td>
<td align="right">0.1 - 1.5</td>
<td align="right">10 - 20</td>
</tr>
<tr class="odd">
<td align="left"><code>ConcurrentHashMapWithTimedEviction</code> with <code>DelayedTaskEvictionScheduler</code></td>
<td align="right">0.1 - 1.0</td>
<td align="right">9 - 11</td>
</tr>
<tr class="even">
<td align="left"><code>ConcurrentHashMapWithTimedEviction</code> with <code>SingleThreadEvictionScheduler</code></td>
<td align="right">0.1 - 1.0</td>
<td align="right">9 - 11</td>
</tr>
</tbody>
</table>

As can be seen from the above table, `ExecutorServiceEvictionScheduler` delivers the worst Put performance, with Guava coming next and the three queue-based eviction schedulers clearly outperforming Guava. There is no clear winner among the last three. Each of them would deliver optimal performance in different circumstances, for example:

* `RegularTaskEvictionScheduler` has very fast scheduling and cancellation but executes its eviction task at a regular interval, potentially without need. This implementation would deliver optimal performance if scheduling and cancellation are made often and entries expire also often, roughly every 1-10 milliseconds. 
* `DelayedTaskEvictionScheduler` and `SingleThreadEvictionScheduler` are slower during scheduling and cancellation, but the eviction task will run exactly at the time of the next scheduled eviction, not sooner or later. These implementations would deliver optimal performance if scheduling and cancellation are made less often and entries expire at irregular intervals ranging from 1 to hundreds milliseconds.

Between `DelayedTaskEvictionScheduler` and `SingleThreadEvictionScheduler`, one would expect that `SingleThreadEvictionScheduler` would deliver better performance, since it is a lower-level minimalistic implementation. However, this is not yet confirmed by testing. It seems cancellation and scheduling in a `ScheduledExecutorService` are really cheap and the two schedulers perform roughly equally.

In addition, `RegularTaskEvictionScheduler` has the advantage over the other two that by specifying a higher delay than 1 millisecond you could trade lower eviction accuracy for higher performance, as its regular task would then fire less often.

## Design

### Overview

For a more comprehensive overview of the library design, see this [class diagram](Evictor/raw/master/design/design.png).

### Interfaces

The basic interface was part of the [contest task description](http://www.cayetanogaming.com/javatask). Some things are done a bit differently than requested, for a good reason:

* The map is parameterized `<K, V>`, which is more useful than a map accepting `Object` as key or value.
* The main interface `ConcurrentMapWithTimedEviction` extends `java.util.concurrent.ConcurrentMap`, which again is very useful. This however forces the implementation of more methods than requested.
* Besides the methods `put` and `putIfAbsent`, there are also `evictMs` versions of the 2 `replace` methods in `ConcurrentMap`. Since the interface extends `ConcurrentMap`, it is natural all methods that can put something in the map to have `evictMs` versions.

### Composability

Composability is heavily favored and therefore as a user of the library, you have the flexibility to decorate a different class than the obvious `java.util.concurrent.ConcurrentHashMap` or to choose a different (e.g. faster) priority queue implementation in the eviction schedulers than `java.util.concurrent.ConcurrentSkipListMap`. In practice, it would be hard to find a faster concurrent hash map than `ConcurrentHashMap` or a faster concurrent priority queue than `ConcurrentSkipListMap`. However, if such would exist, they could easily be used with the library.

### Thread Safety

Thread safety is ensured via the following simple approach:

* All used data structures are thread safe, e.g. `java.util.concurrent.ConcurrentHashMap`
* All class fields are either `final` (when possible) or `volatile`
* A small number of private methods in the schedulers are `synchronized` to ensure atomicity in a concurrent environment

## Tests

There are three test classes provided with the library:

* `ConcurrentMapWithTimedEvictionTest` tests the functional correctness of the different `ConcurrentMapWithTimedEviction` implementations in single-threaded and multi-threaded environment.
* `ConcurrentMapWithTimedEvictionAccuracyTest` tests the *accuracy* of the different eviction schedulers, i.e. how close are the actual eviction times to the times specified via the `evictMs` parameter. Here, an average accuracy of 400-600 microseconds is considered optimal. 
* `ConcurrentMapWithTimedEvictionPerfTest` tests the performance of the different `ConcurrentMapWithTimedEviction` implementations under an artificially heavy load

Note that `ConcurrentMapWithTimedEvictionTest` sometimes (but very rarely) fails due to unpredictible lower accuracy of single evictions. 

See:
* [ConcurrentMapWithTimedEvictionTest.java](Evictor/blob/master/evictor/test/com/stoyanr/evictor/ConcurrentMapWithTimedEvictionTest.java)
* [ConcurrentMapWithTimedEvictionAccuracyTest.java](Evictor/blob/master/evictor/test/com/stoyanr/evictor/ConcurrentMapWithTimedEvictionAccuracyTest.java)
* [ConcurrentMapWithTimedEvictionPerfTest.java](Evictor/blob/master/evictor/test/com/stoyanr/evictor/ConcurrentMapWithTimedEvictionPerfTest.java)

## Build and Development Environment

There is a Maven build which you could use to rebuild the library if needed. Just do `mvn clean install`, or `mvn clean install -DskipTests` if you would rather skip the tests. The only external dependencies used in the project are JUnit and Guava, and these are used only for testing. The actual jar built has no external dependencies whatsoever. Besides the binary package, the Maven build produces also source and Javadoc packages.

There is also an Eclipse project. If you import it in Eclipse, make sure that you have the Maven Eclipse plugin (m2e) installed.
