package com.github.mimsic.pcp.handler;

import com.github.mimsic.pcp.util.LoggerUtil;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Processor<T> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    private final Handler<T> handler;
    private final Runnable process;
    private final Regulator regulator;

    public Processor(Handler<T> handler, int minThreads) {

        this.handler = handler;
        this.process = this::process;
        this.regulator = new StandardRegulator(minThreads);
    }

    public Processor(Handler<T> handler, int maxThreads, int minThreads, long maxRate) {

        this.handler = handler;
        this.process = this::process;
        this.regulator = new ScheduledRegulator<>(this, maxThreads, minThreads, maxRate);

        Optional.ofNullable(handler.schedule(regulator::regulate, 1000, 1000, TimeUnit.MILLISECONDS))
                .orElseThrow(RuntimeException::new);
    }

    public Handler<T> handler() {

        return handler;
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
                    handler.submit(process);
                    regulator.add();
                } catch (Exception e) {
                    LoggerUtil.error(handler.getClass(), "", e);
                    regulator.release();
                    regulator.remove();
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
                regulator.done();
            }
        }
    }

    private T consume() {

        T data;

        regulator.check();

        if (Thread.interrupted()) {
            regulator.remove();
            return null;
        }
        if ((data = queue.poll()) == null) {
            lock.writeLock().lock();
            try {
                if ((data = queue.poll()) == null) {
                    regulator.release();
                    regulator.remove();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return data;
    }
}
