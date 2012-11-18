package com.stoyanr.evictor;

public class SingleThreadEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    private volatile boolean finished = false;
    private volatile boolean notified = false;
    private volatile long next = 0;
    private final Thread t = new Thread(new EvictionThread());
    private final Object m = new Object();

    public SingleThreadEvictionScheduler() {
        super();
        t.start();
    }

    public SingleThreadEvictionScheduler(EvictionQueue<K, V> queue) {
        super(queue);
        t.start();
    }

    @Override
    public void shutdown() {
        finished = true;
        t.interrupt();
        try {
            t.join();
        } catch (InterruptedException e) {
        }
    }

    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            synchronized (m) {
                notified = true;
                m.notifyAll();
            }
        }
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        if (getNextEvictionTime() != next) {
            synchronized (m) {
                notified = true;
                m.notifyAll();
            }
        }
    }

    @Override
    protected void onEvictEntries() {
        // Do nothing
    }

    final class EvictionThread implements Runnable {

        @Override
        public void run() {
            while (!finished) {
                next = getNextEvictionTime();
                long timeout = calcTimeout(next);
                while (timeout >= 0) {
                    if (!waitFor(timeout) && !finished) {
                        // The timeout did not expire and we are not finished - 
                        // calculate the next timeout
                        next = getNextEvictionTime();
                        timeout = calcTimeout(next);
                    } else {
                        // The timeout expired or we are finished - get out
                        break;
                    }
                }
                evictEntries();
            }
        }

        private long calcTimeout(long time) {
            if (time > 0) {
                long x = time - System.nanoTime();
                return (x != 0) ? x : -1;
            } else {
                return 0;
            }
        }

        private boolean waitFor(long timeout) {
            boolean result = true;
            try {
                synchronized (m) {
                    notified = false;
                    m.wait(timeout / 1000000, (int) (timeout % 1000000));
                    result = !notified;
                }
            } catch (InterruptedException e) {
                result = false;
            }
            return result;
        }
    }
    
}
