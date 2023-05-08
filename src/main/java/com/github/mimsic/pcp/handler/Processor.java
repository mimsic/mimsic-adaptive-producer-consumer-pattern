package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.util.Log;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Processor<T> {

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Semaphore terminator = new Semaphore(0);
    private final Semaphore regulator = new Semaphore(1);
    private final LongAdder rate = new LongAdder();
    private final LongAdder threads = new LongAdder();

    private final ExecutorService executor;
    private final Handler<T> handler;
    private final Runnable process;
    private final AtomicInteger maxThreads;
    private final AtomicInteger maxRate;
    private final AtomicLong maxLatency;

    public Processor(
            ExecutorService executor,
            ScheduledExecutorService scheduledExecutor,
            Handler<T> handler,
            int maxThreads,
            int maxRate,
            long maxLatency,
            TimeUnit timeUnit) {

        this.executor = executor;
        this.handler = handler;
        this.process = this::process;

        this.maxThreads = new AtomicInteger(maxThreads);
        this.maxRate = new AtomicInteger(maxRate);
        this.maxLatency = new AtomicLong(timeUnit.toMillis(maxLatency));

        scheduledExecutor.scheduleAtFixedRate(this::regulate, 1000, 1000, TimeUnit.MILLISECONDS);

        Log.info(
                handler.getClass(),
                "Processor started with a max threads of {} a maxRate of {} and a max latency of {}",
                maxThreads,
                maxRate,
                maxLatency);
    }

    public Processor<T> setMaxThreads(int maxThreads) {
        this.maxThreads.set(maxThreads);
        return this;
    }

    public Processor<T> setMaxRate(int maxRate) {
        this.maxRate.set(maxRate);
        return this;
    }

    public Processor<T> setMaxLatency(long maxLatency, TimeUnit timeUnit) {
        this.maxLatency.set(timeUnit.toMillis(maxLatency));
        return this;
    }

    public void queue(List<T> items) {

        queue.addAll(items);
        execute();
    }

    public void queue(T item) {

        queue.offer(item);
        execute();
    }

    private void execute() {

        lock.readLock().lock();
        try {
            if (regulator.tryAcquire(0, TimeUnit.NANOSECONDS)) {
                try {
                    executor.submit(process);
                    threads.increment();
                } catch (Exception e) {
                    Log.error(handler.getClass(), "", e);
                    regulator.release();
                }
            }
        } catch (InterruptedException e) {
            Log.error(handler.getClass(), "", e);
            Thread.currentThread().interrupt();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void process() {

        T data;

        while ((data = consume()) != null) {

            try {
                handler.process(data);
            } catch (InterruptedException e) {
                Log.error(handler.getClass(), "", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.error(handler.getClass(), "", e);
            } finally {
                rate.increment();
            }
        }
    }

    private T consume() {

        T data;

        if (Thread.currentThread().isInterrupted() || terminator.tryAcquire()) {
            threads.decrement();
            return null;
        }
        if ((data = queue.poll()) == null) {
            lock.writeLock().lock();
            try {
                if ((data = queue.poll()) == null) {
                    regulator.release();
                    threads.decrement();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return data;
    }

    private void regulate() {

        long totalRate = rate.sumThenReset();
        long totalThreads = threads.sum();

        if (totalThreads > 0) {

            long avgRatePerThread = totalRate / totalThreads;
            long avgLatency = 1000 / avgRatePerThread;
            long estimatedThreads = totalThreads + 1;
            long estimatedRate = avgRatePerThread * estimatedThreads;

            Log.info(handler.getClass(),
                    "Total rate of {} with {} threads, " +
                            "average rate of {} and latency of {} per thread, " +
                            "estimated rate of {} with {} threads",
                    totalRate,
                    totalThreads,
                    avgRatePerThread,
                    avgLatency,
                    estimatedRate,
                    estimatedThreads);

            if (avgLatency < maxLatency.get() && estimatedRate < maxRate.get() && totalThreads < maxThreads.get()) {
                regulator.release();
                this.execute();
            } else if ((avgLatency > (maxLatency.get() * 1.05) || totalRate > (maxRate.get() * 1.05)) && totalThreads > 1) {
                terminator.release();
            }
        }
    }
}
