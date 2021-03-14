package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.util.LoggerUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Processor<T> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    private final Semaphore terminator = new Semaphore(0);
    private final Semaphore regulator;

    private final LongAdder rate = new LongAdder();
    private final LongAdder threads = new LongAdder();

    private final Handler<T> handler;
    private final Runnable process;

    private final int maxThreads;
    private final long maxRate;

    public Processor(Handler<T> handler, int maxThreads, int minThreads, long maxRate) {

        this.handler = handler;
        this.process = this::process;
        this.regulator = new Semaphore(minThreads);

        this.maxThreads = maxThreads;
        this.maxRate = maxRate;

        Optional.ofNullable(handler.schedule(this::regulate, 1000, 1000, TimeUnit.MILLISECONDS))
                .orElseThrow(RuntimeException::new);
    }

    public void queue(List<T> items) {

        queue.addAll(items);
        execute();
    }

    public void queue(T item) {

        queue.offer(item);
        execute();
    }

    public void execute() {

        lock.readLock().lock();
        try {
            if (regulator.tryAcquire(0, TimeUnit.SECONDS)) {
                try {
                    Optional.ofNullable(handler.submit(process)).orElseThrow(RuntimeException::new);
                    threads.increment();
                } catch (Exception e) {
                    LoggerUtil.error(handler.getClass(), "", e);
                    regulator.release();
                    threads.decrement();
                }
            }
        } catch (InterruptedException e) {
            LoggerUtil.error(handler.getClass(), "", e);
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
                LoggerUtil.error(handler.getClass(), "", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LoggerUtil.error(handler.getClass(), "", e);
            } finally {
                rate.increment();
            }
        }
    }

    private T consume() {

        T data;

        if (terminator.tryAcquire()) {
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

    public void regulate() {

        long totalRate = rate.sumThenReset();
        long totalThreads = threads.sum();

        if (totalThreads > 0) {

            long estimatedRate = (totalRate / totalThreads) * (totalThreads + 1);
            LoggerUtil.info(
                    handler.getClass(),
                    "totalThreads: {}, totalRate: {}, estimatedRate: {}",
                    totalThreads,
                    totalRate,
                    estimatedRate);

            if (estimatedRate < maxRate && totalThreads < maxThreads) {
                regulator.release();
                this.execute();
            } else if (totalRate > maxRate && totalThreads > 1) {
                terminator.release();
            }
        }
    }
}
