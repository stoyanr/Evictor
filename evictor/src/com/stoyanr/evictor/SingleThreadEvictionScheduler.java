package com.stoyanr.evictor;

public class SingleThreadEvictionScheduler<K, V> extends AbstractQueueEvictionScheduler<K, V> {

    private volatile boolean finished = false;
    private final Object monitor = new Object();

    public SingleThreadEvictionScheduler() {
        super();
        startThread();
    }

    public SingleThreadEvictionScheduler(EvictionQueue<K, V> queue) {
        super(queue);
        startThread();
    }

    private void startThread() {
        new Thread(new EvictionThread()).start();
    }

    @Override
    public void shutdown() {
        finished = true;
    }

    @Override
    protected void onScheduleEviction(EvictibleEntry<K, V> e) {
        synchronized (monitor) {
            monitor.notify();
        }
    }

    @Override
    protected void onCancelEviction(EvictibleEntry<K, V> e) {
        // Do nothing
    }

    @Override
    protected void onEvictEntries() {
        // Do nothing
    }

    final class EvictionThread implements Runnable {

        @Override
        public void run() {
            while (!finished) {
                long timeout = -1;
                while (timeout != 0) {
                    timeout = calcTimeout(getNextEvictionTime());
                    waitFor(timeout);
                }
                evictEntries();
            }
        }

        private long calcTimeout(long time) {
            return (time > 0) ? Math.max(time - System.nanoTime(), 0) : -1;
        }

        private void waitFor(long timeout) {
            try {
                if (timeout > 0) {
                    synchronized (monitor) {
                        monitor.wait(timeout / 1000000, (int) (timeout % 1000000));
                    }
                } else if (timeout < 0) {
                    synchronized (monitor) {
                        monitor.wait();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

}
