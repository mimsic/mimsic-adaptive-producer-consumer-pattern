package com.github.mimsic.pcp.handler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ItemProcessor<T> {

    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final ItemHandler<T> handler;
    private final Semaphore semaphore;
    private final Runnable runnable;

    public ItemProcessor(ItemHandler<T> handler, int permits) {

        this.handler = handler;
        this.semaphore = new Semaphore(permits);
        this.runnable = this::runnable;
    }

    public void queue(T item) {

        queue.offer(item);

        lock.readLock().lock();
        try {
            if (semaphore.tryAcquire(0, TimeUnit.SECONDS)) {
                try {
                    handler.execute(runnable);
                } catch (Exception e) {
                    handler.logger(e);
                    semaphore.release();
                }
            }
        } catch (InterruptedException e) {
            handler.logger(e);
            Thread.currentThread().interrupt();
        } finally {
            lock.readLock().unlock();
        }
    }

    private void runnable() {

        T item;

        while (!Thread.currentThread().isInterrupted() && (item = tryConsume()) != null) {

            try {
                handler.process(item);
            } catch (Exception e) {
                handler.logger(e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private T tryConsume() {

        T item;

        if ((item = queue.poll()) == null) {
            lock.writeLock().lock();
            try {
                if ((item = queue.poll()) == null) {
                    semaphore.release();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
        return item;
    }
}
